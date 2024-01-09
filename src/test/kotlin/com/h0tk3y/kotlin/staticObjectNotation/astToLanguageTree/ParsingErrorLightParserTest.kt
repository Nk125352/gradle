package com.example.com.h0tk3y.kotlin.staticObjectNotation.astToLanguageTree

import com.h0tk3y.kotlin.staticObjectNotation.astToLanguageTree.AbstractRejectedLanguageFeaturesTest
import com.h0tk3y.kotlin.staticObjectNotation.astToLanguageTree.ElementResult
import com.h0tk3y.kotlin.staticObjectNotation.astToLanguageTree.ParseTestUtil
import org.junit.jupiter.api.Test

class ParsingErrorLightParserTest: AbstractRejectedLanguageFeaturesTest() {

    override fun parse(code: String): List<ElementResult<*>> = ParseTestUtil.parseWithLightParser(code)

    @Test
    fun `single unparsable expression`() {
        val code = "."

        val expected = """
            ParsingError(
                message = Expecting an element, 
                potentialElementSource = indexes: 0..1, line/column: 1/1..1/2, file: test, 
                erroneousSource = indexes: 0..1, line/column: 1/1..1/2, file: test
            )
            """.trimIndent()
        assertResult(expected, code)
    }

    @Test
    fun `unexpected statement separator inside function call`() {
        val code = "id(\"plugin-id-1\";)"

        val expected = """
            ParsingError(
                message = Unparsable value argument: "("plugin-id-1"". Expecting ')', 
                potentialElementSource = indexes: 2..16, line/column: 1/3..1/17, file: test, 
                erroneousSource = indexes: 16..16, line/column: 1/17..1/17, file: test
            )
            ParsingError(
                message = Expecting an element, 
                potentialElementSource = indexes: 17..18, line/column: 1/18..1/19, file: test, 
                erroneousSource = indexes: 17..18, line/column: 1/18..1/19, file: test
            )
            """.trimIndent()
        assertResult(expected, code)
    }

    @Test
    fun `missing closing parenthesis in function argument`() {
        val code = "kotlin(\"plugin-id-1) ; kotlin(\"plugin-id-2\")"

        val expected = """
            MultipleFailures(
                MultipleFailures(
                    UnsupportedConstruct(
                        languageFeature = PrefixExpression, 
                        potentialElementSource = indexes: 37..40, line/column: 1/38..1/41, file: test, 
                        erroneousSource = indexes: 37..40, line/column: 1/38..1/41, file: test
                    )
                    UnsupportedConstruct(
                        languageFeature = UnsupportedOperator, 
                        potentialElementSource = indexes: 40..41, line/column: 1/41..1/42, file: test, 
                        erroneousSource = indexes: 40..41, line/column: 1/41..1/42, file: test
                    )
                )
                ParsingError(
                    message = Unparsable value argument: "("plugin-id-1) ; kotlin("plugin-id-2")". Expecting ',', 
                    potentialElementSource = indexes: 6..44, line/column: 1/7..1/45, file: test, 
                    erroneousSource = indexes: 42..42, line/column: 1/43..1/43, file: test
                )
                ParsingError(
                    message = Unparsable value argument: "("plugin-id-1) ; kotlin("plugin-id-2")". Expecting ')', 
                    potentialElementSource = indexes: 6..44, line/column: 1/7..1/45, file: test, 
                    erroneousSource = indexes: 44..44, line/column: 1/45..1/45, file: test
                )
            )""".trimIndent()
        assertResult(expected, code)
    }

    @Test
    fun `missing assignment in one of a series of assignments`() {
        val code = """
            val a = 1
            val b = 2
            val c 3
            val d = 4
            val e = 5
        """.trimIndent()

        val expected = """
            LocalValue [indexes: 0..9, line/column: 1/1..1/10, file: test] (
                name = a
                rhs = IntLiteral [indexes: 8..9, line/column: 1/9..1/10, file: test] (1)
            )
            LocalValue [indexes: 10..19, line/column: 2/1..2/10, file: test] (
                name = b
                rhs = IntLiteral [indexes: 18..19, line/column: 2/9..2/10, file: test] (2)
            )
            UnsupportedConstruct(
                languageFeature = UninitializedProperty, 
                potentialElementSource = indexes: 20..25, line/column: 3/1..3/6, file: test, 
                erroneousSource = indexes: 20..25, line/column: 3/1..3/6, file: test
            )
            ParsingError(
                message = Unexpected tokens (use ';' to separate expressions on the same line), 
                potentialElementSource = indexes: 26..27, line/column: 3/7..3/8, file: test, 
                erroneousSource = indexes: 26..27, line/column: 3/7..3/8, file: test
            )
            LocalValue [indexes: 28..37, line/column: 4/1..4/10, file: test] (
                name = d
                rhs = IntLiteral [indexes: 36..37, line/column: 4/9..4/10, file: test] (4)
            )
            LocalValue [indexes: 38..47, line/column: 5/1..5/10, file: test] (
                name = e
                rhs = IntLiteral [indexes: 46..47, line/column: 5/9..5/10, file: test] (5)
            )
            """.trimIndent()
        assertResult(expected, code)
    }

    @Test
    fun `missing parenthesis in one of a series of assignments`() {
        val code = """
            val a = 1
            val b = (2
            val c = 9
        """.trimIndent()

        val expected = """
            LocalValue [indexes: 0..9, line/column: 1/1..1/10, file: test] (
                name = a
                rhs = IntLiteral [indexes: 8..9, line/column: 1/9..1/10, file: test] (1)
            )
            ParsingError(
                message = Expecting ')', 
                potentialElementSource = indexes: 18..20, line/column: 2/9..2/11, file: test, 
                erroneousSource = indexes: 20..20, line/column: 2/11..2/11, file: test
            )
            LocalValue [indexes: 21..30, line/column: 3/1..3/10, file: test] (
                name = c
                rhs = IntLiteral [indexes: 29..30, line/column: 3/9..3/10, file: test] (9)
            )
            """.trimIndent()
        assertResult(expected, code)
    }

    @Test
    fun `accidentally concatenated lines in a series of assignments`() {
        val code = """
            val a = 1
            val b = 2 val c = 3
            val d = 4
        """.trimIndent()

        val expected = """
            LocalValue [indexes: 0..9, line/column: 1/1..1/10, file: test] (
                name = a
                rhs = IntLiteral [indexes: 8..9, line/column: 1/9..1/10, file: test] (1)
            )
            LocalValue [indexes: 10..19, line/column: 2/1..2/10, file: test] (
                name = b
                rhs = IntLiteral [indexes: 18..19, line/column: 2/9..2/10, file: test] (2)
            )
            ParsingError(
                message = Unexpected tokens (use ';' to separate expressions on the same line), 
                potentialElementSource = indexes: 19..19, line/column: 2/10..2/10, file: test, 
                erroneousSource = indexes: 19..19, line/column: 2/10..2/10, file: test
            )
            LocalValue [indexes: 20..29, line/column: 2/11..2/20, file: test] (
                name = c
                rhs = IntLiteral [indexes: 28..29, line/column: 2/19..2/20, file: test] (3)
            )
            LocalValue [indexes: 30..39, line/column: 3/1..3/10, file: test] (
                name = d
                rhs = IntLiteral [indexes: 38..39, line/column: 3/9..3/10, file: test] (4)
            )
            """.trimIndent()
        assertResult(expected, code)
    }

    @Test
    fun `internal error in a block`() {
        val code = """
            block {
                val a = 1
                b = 2
                val c 3
                d = 4
            }
        """.trimIndent()

        val expected = """
            MultipleFailures(
                UnsupportedConstruct(
                    languageFeature = UninitializedProperty, 
                    potentialElementSource = indexes: 36..41, line/column: 4/5..4/10, file: test, 
                    erroneousSource = indexes: 36..41, line/column: 4/5..4/10, file: test
                )
                ParsingError(
                    message = Unexpected tokens (use ';' to separate expressions on the same line), 
                    potentialElementSource = indexes: 42..43, line/column: 4/11..4/12, file: test, 
                    erroneousSource = indexes: 42..43, line/column: 4/11..4/12, file: test
                )
            )
            """.trimIndent()
        assertResult(expected, code)

        // TODO: atm a block can't be made up of a mixture of errors and successes
    }
}