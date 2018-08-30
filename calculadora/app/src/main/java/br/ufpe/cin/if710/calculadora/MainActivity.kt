package br.ufpe.cin.if710.calculadora

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.view.View
import android.widget.Button
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : Activity(), View.OnClickListener {

    private lateinit var digitIds: List<Int>
    private lateinit var opIds: List<Int>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val digitButtons = listOf(btn_0, btn_1, btn_2, btn_3, btn_4, btn_5, btn_6, btn_7,
                btn_8, btn_9)
        val opButtons = listOf(btn_Dot, btn_Add, btn_Subtract, btn_Multiply, btn_Divide, btn_Power,
                btn_LParen, btn_RParen)

        digitIds = digitButtons.map { it.id }
        opIds = opButtons.map { it.id }
        opButtons.forEach { it.setOnClickListener(this) }
        digitButtons.forEach { it.setOnClickListener(this) }
        btn_Clear.setOnClickListener(this)
        btn_Equal.setOnClickListener(this)

        if (savedInstanceState != null) {
            text_info.text = savedInstanceState.getString("textInfo").toEditable()
            text_calc.text = savedInstanceState.getString("textCalc").toEditable()
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putString("textInfo", text_info.text.toString())
        outState?.putString("textCalc", text_calc.text.toString())
    }

    // listener para o clique de cada botao
    override fun onClick(v: View?) {
        val button: Button = v as Button

        when (button.id) {
            in digitIds, in opIds -> text_calc.append(button.text)
            R.id.btn_Clear -> {
                val expr = text_calc.text.toString()
                text_calc.text = expr.dropLast(1).toEditable()
            }
            else -> {
                val result = eval(text_calc.text.toString())
                text_info.text = result?.toString()
            }
        }
    }

    private fun String.toEditable(): Editable {
        return Editable.Factory.getInstance().newEditable(this)
    }

    private fun Any.toast(context: Context, duration: Int = Toast.LENGTH_LONG) {
        Toast.makeText(context, this.toString(), duration).show()
    }

    //Como usar a função:
    // eval("2+2") == 4.0
    // eval("2+3*4") = 14.0
    // eval("(2+3)*4") = 20.0
    //Fonte: https://stackoverflow.com/a/26227947
    fun eval(str: String): Double? {
        return object : Any() {
            var pos = -1
            var ch: Char = ' '
            fun nextChar() {
                val size = str.length
                ch = if ((++pos < size)) str.get(pos) else (-1).toChar()
            }

            fun eat(charToEat: Char): Boolean {
                while (ch == ' ') nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double? {
                nextChar()
                var x: Double? = parseExpression()
                if (pos < str.length) {
                    x = null
                    "Caractere inesperado: $ch".toast(this@MainActivity)
                }
                return x
            }

            // Grammar:
            // expression = term | expression `+` term | expression `-` term
            // term = factor | term `*` factor | term `/` factor
            // factor = `+` factor | `-` factor | `(` expression `)`
            // | number | functionName factor | factor `^` factor
            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'))
                        x += parseTerm() // adição
                    else if (eat('-'))
                        x -= parseTerm() // subtração
                    else
                        return x
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'))
                        x *= parseFactor() // multiplicação
                    else if (eat('/'))
                        x /= parseFactor() // divisão
                    else
                        return x
                }
            }

            fun parseFactor(): Double {
                if (eat('+')) return parseFactor() // + unário
                if (eat('-')) return -parseFactor() // - unário
                var x: Double
                val startPos = this.pos
                if (eat('(')) { // parênteses
                    x = parseExpression()
                    eat(')')
                } else if ((ch in '0'..'9') || ch == '.') { // números
                    while ((ch in '0'..'9') || ch == '.') nextChar()
                    x = java.lang.Double.parseDouble(str.substring(startPos, this.pos))
                } else if (ch in 'a'..'z') { // funções
                    while (ch in 'a'..'z') nextChar()
                    val func = str.substring(startPos, this.pos)
                    x = parseFactor()
                    if (func == "sqrt")
                        x = Math.sqrt(x)
                    else if (func == "sin")
                        x = Math.sin(Math.toRadians(x))
                    else if (func == "cos")
                        x = Math.cos(Math.toRadians(x))
                    else if (func == "tan")
                        x = Math.tan(Math.toRadians(x))
                    else {
                        x = 0.0
                        "Função desconhecida: $func".toast(this@MainActivity)
                    }
                } else {
                    x = 0.0
                    "Caractere inesperado: $ch".toast(this@MainActivity)
                }
                if (eat('^')) x = Math.pow(x, parseFactor()) // potência
                return x
            }
        }.parse()
    }

}
