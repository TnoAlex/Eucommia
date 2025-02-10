// auto generated, do not manually edit!
package com.github.tnoalex.handle.generated

import com.github.tnoalex.handle.BaseHandlerTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.BeforeAll
import com.github.gumtreediff.client.Run

class HandlerTest : BaseHandlerTest() {
   
    companion object {
        @JvmStatic
        @BeforeAll
        fun init() {
            Run.initGenerators()
        }
    }
    
    @Nested
    inner class NonNull2NullableTest {
        @Test
        fun test_NonNull2Nullable1() {
            doValidate(
                """testData/NonNull2Nullable/NonNull2Nullable1""",
                """new.kt""",
                """old.kt""",
                listOf<String>("non_null_2_nullable")
            )
        }
    
        @Test
        fun test_NonNull2Nullable2() {
            doValidate(
                """testData/NonNull2Nullable/NonNull2Nullable2""",
                """new.kt""",
                """old.kt""",
                listOf<String>("non_null_2_nullable", "add_null_safe_operator")
            )
        }
    
    
    }
        
    @Nested
    inner class AddIfExpNullSafeTestTest {
        @Test
        fun test_addIfExpNull1() {
            doValidate(
                """testData/addIfExpNullSafeTest/addIfExpNull1""",
                """test.kt""",
                """test_old.kt""",
                listOf<String>("add_if_exp_for_null_safe")
            )
        }
    
        @Test
        fun test_addIfExpNull2() {
            doValidate(
                """testData/addIfExpNullSafeTest/addIfExpNull2""",
                """new.kt""",
                """old.kt""",
                listOf<String>("add_if_exp_for_null_safe")
            )
        }
    
    
    }
        
    @Nested
    inner class AddNotNullAnnotationTestTest {
        @Test
        fun test_addNotNullAnnotation1() {
            doValidate(
                """testData/addNotNullAnnotationTest/addNotNullAnnotation1""",
                """test.java""",
                """test_old.java""",
                listOf<String>("add_not_null_annotation")
            )
        }
    
    
    }
        
    @Nested
    inner class AddNullAsseertTestTest {
        @Test
        fun test_addNullAsseert1() {
            doValidate(
                """testData/addNullAsseertTest/addNullAsseert1""",
                """test.kt""",
                """test_old.kt""",
                listOf<String>("add_null_assertion_operator")
            )
        }
    
    
    }
        
    @Nested
    inner class AddNullSafeTestTest {
        @Test
        fun test_addNullSafe1() {
            doValidate(
                """testData/addNullSafeTest/addNullSafe1""",
                """test.kt""",
                """test_old.kt""",
                listOf<String>("add_null_safe_operator")
            )
        }
    
    
    }
        
    @Nested
    inner class AddNullableAnnotationTestTest {
        @Test
        fun test_addNullableAnnotation1() {
            doValidate(
                """testData/addNullableAnnotationTest/addNullableAnnotation1""",
                """test.java""",
                """test_old.java""",
                listOf<String>("add_null_able_annotation")
            )
        }
    
    
    }
        
    @Nested
    inner class AddThrowsAnnotationTestTest {
        @Test
        fun test_addThrowsAnnotation1() {
            doValidate(
                """testData/addThrowsAnnotationTest/addThrowsAnnotation1""",
                """test.kt""",
                """test_old.kt""",
                listOf<String>("exception_mark")
            )
        }
    
    
    }
    
}