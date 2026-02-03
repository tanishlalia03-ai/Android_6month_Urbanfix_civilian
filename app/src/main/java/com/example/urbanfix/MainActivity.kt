package com.example.urbanfix

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.urbanfix.Bottomnavigation.BottomLayoutActivity
import com.example.urbanfix.Constraintfiles.Constraint2Activity
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {
    lateinit var log : Button
    lateinit var e1 : EditText
    lateinit var pass : EditText

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }
        log=findViewById<Button>(R.id.btnLogin)
        e1=findViewById<TextInputEditText>(R.id.etEmail)


        pass=findViewById<TextInputEditText>(R.id.etPassword)

        log.setOnClickListener {
            var e2 = e1.text.toString()
            var pass2= pass.text.toString()

            if(e2.toString() == "tanishlalia@gmail.com" && pass2.toString()=="123"){

                val intent = Intent(this, BottomLayoutActivity:: class.java)
                startActivity(intent)
            }

            else {
                val toast = Toast.makeText(this, "Please enter correct", Toast.LENGTH_SHORT).show()
            }
        }



    }
}