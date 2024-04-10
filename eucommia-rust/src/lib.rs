use std::error::Error;
use std::fmt::{Debug, Display, format, Formatter};
use std::ops::Deref;
use std::path::PathBuf;
use std::process::exit;

use git2::{Delta, Repository};
use jni::JNIEnv;
use jni::objects::{JClass, JObject, JString, JValueGen};

struct GitProcessorError {
    message: &'static str,
}

impl GitProcessorError {
    pub fn new(message: &'static str) -> Self {
        Self { message }
    }
}

impl Debug for GitProcessorError {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        f.write_str("GitProcessorError: ")?;
        f.write_str(self.message)
    }
}

impl Display for GitProcessorError {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        f.write_str("GitProcessorError: ")?;
        f.write_str(self.message)
    }
}

impl Error for GitProcessorError {}

#[no_mangle]
pub extern "system" fn Java_com_github_eucommia_GitProcessor_visitGitRepo<'local>(
    mut env: JNIEnv<'local>,
// This is the class that owns our static method. It's not going to be used,
// but still must be present to match the expected signature of a static
// native method.
    _class: JClass<'local>,
    path: JString<'local>,
    visitor: JObject<'local>,
) {
    let result = visit_git_repo(&mut env, path, visitor);
    if result.is_err() {
        let string = format!("Error: {:?}", &result);
/*        let x = result.err().unwrap();
        if x.is::<jni::errors::Error>() {
            let x1 = x.downcast_ref::<jni::errors::Error>().unwrap();
            match x1 {
                jni::errors::Error::WrongJValueType(_, _) => {}
                jni::errors::Error::InvalidCtorReturn => {}
                jni::errors::Error::InvalidArgList(_) => {}
                jni::errors::Error::MethodNotFound { .. } => {}
                jni::errors::Error::FieldNotFound { .. } => {}
                jni::errors::Error::JavaException => {

                }
                jni::errors::Error::JNIEnvMethodNotFound(_) => {}
                jni::errors::Error::NullPtr(_) => {}
                jni::errors::Error::NullDeref(_) => {}
                jni::errors::Error::TryLock => {}
                jni::errors::Error::JavaVMMethodNotFound(_) => {}
                jni::errors::Error::FieldAlreadySet(_) => {}
                jni::errors::Error::ThrowFailed(_) => {}
                jni::errors::Error::ParseFailed(_, _) => {}
                jni::errors::Error::JniCall(_) => {}
            }
        }*/
        eprintln!("{}", string);
        env.throw_new("java/lang/Exception", string).unwrap();
    }
}

fn visit_git_repo<'local>(
    env: &mut JNIEnv<'local>, path: JString<'local>, visitor: JObject<'local>,
) -> Result<i32, Box<dyn Error>> {
    let path = env.get_string(&path)?;
    let path = PathBuf::from(path.to_str()?);
    let repo = Repository::open(&path)?;
    let mut revwalk = repo.revwalk()?;
    revwalk.set_sorting(git2::Sort::TIME)?;
    revwalk.push_head()?;
    while let Some(oid) = revwalk.next() {
        if let Ok(oid) = oid {
            let commit = repo.find_commit(oid)?;
            if commit.parent_count() != 1 { continue; }
            let parent = commit.parent(0)?;
            let diff = repo.diff_tree_to_tree(
                Some(&commit.tree()?), Some(&parent.tree()?), None)?;
            let deltas = diff.deltas();
            let mut nfiles = 0;
            diff.foreach(
                &mut |_, _| {
                    nfiles += 1;
                    true
                },
                None,
                None,
                None)?;
            if nfiles > 10 { continue; }
            let mut java_modified = false;
            let mut kt_modified = false;
            for delta in deltas {
                if delta.status() != Delta::Modified { continue; }
                let file_path = delta.new_file().path()
                    .ok_or(GitProcessorError::new("file path error"))?
                    .to_str()
                    .ok_or(GitProcessorError::new("file path to str error"))?;
                let java_file = file_path.ends_with(".java");
                let kt_file = file_path.ends_with(".kt");
                if java_file { java_modified = true; }
                if kt_file { kt_modified = true; }
                if !kt_file && !java_file { continue; }
                let new_blob = repo.find_blob(delta.new_file().id());
                if new_blob.is_err() { continue; }
                let new_blob = new_blob?;
                let old_blob = repo.find_blob(delta.old_file().id());
                if old_blob.is_err() { continue; }
                let old_blob = old_blob?;
                let commit_id_java_str = env.new_string(commit.id().to_string())?;
                let file_path_java_str = env.new_string(file_path)?;
                let new_blob_java_str = env.new_string(String::from_utf8_lossy(new_blob.content()))?;
                let old_blob_java_str = env.new_string(String::from_utf8_lossy(old_blob.content()))?;
                // println!("call visitor:...");
                env.call_method(
                    &visitor, "invoke",
                    "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                    /*&[(&commit_id_java_str).into(),
                        (&file_path_java_str).into(),
                        (&new_blob_java_str).into(),
                        (&old_blob_java_str).into()],*/
                    &[JValueGen::Object(&commit_id_java_str),
                        JValueGen::Object(&file_path_java_str),
                        JValueGen::Object(&new_blob_java_str),
                        JValueGen::Object(&old_blob_java_str)]
                )?;
                // println!("call visitor success");
                if env.exception_check()? {
                    println!("find java exception");
                    let throwable = env.exception_occurred()?;
                    println!("got java exception");
                    let str = env.call_method(throwable, "toString",
                                              "()Ljava/lang/String;", &[])?;
                    eprintln!("error in jvm: {:?}", str.l().unwrap());
                    exit(-1);
                }
            }
        }
    }
    Ok(0)
}