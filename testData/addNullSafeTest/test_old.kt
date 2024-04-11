class Test{
	private lateinit var prop:HashMap<String,String>
	
	init{
		prop = JavaFunc()	
	}
	fun onTest(s:String){
		prop.conatinsKey(s)
	}
}