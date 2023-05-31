package com.example.glideconnect.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.glideconnect.R
import com.example.glideconnect.databinding.ActivityIntroBinding

class IntroActivity : AppCompatActivity() {
     private lateinit var introBinding: ActivityIntroBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        introBinding = ActivityIntroBinding.inflate(layoutInflater)
        setContentView(introBinding.root)

        val toolbar = introBinding.introToolbar
        toolbar.setTitleTextColor(ContextCompat.getColor(applicationContext, R.color.white))
        setSupportActionBar(toolbar)

        supportActionBar?.title = resources.getString(R.string.app_name)

       introBinding.btnEnter.setOnClickListener {
           if (introBinding.etChannelName.text.toString().trim{it <=' ' }.isEmpty()){
               Toast.makeText(this,"PLease Enter A channel Name",Toast.LENGTH_SHORT).show()
           }else{
               val intent =Intent(this, MainActivity::class.java)
               intent.putExtra("CHANNEL NAME",introBinding.etChannelName.text.toString())
               startActivity(intent)
           }
       }
    }



}