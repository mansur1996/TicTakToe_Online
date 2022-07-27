package com.mrmansur.tictactoe2

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.mrmansur.tictactoe2.databinding.ActivityPlayerNameBinding

class PlayerName : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerNameBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerNameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
    }

    private fun initViews() {
        binding.apply {
            startGameBtn.setOnClickListener {
                val getPlayerName = playerNameEt.text.toString()
                if (getPlayerName.isEmpty()){
                    Toast.makeText(this@PlayerName, R.string.str_please_enter_name, Toast.LENGTH_SHORT).show()
                }else{
                    openMainActivity(getPlayerName)
                }
            }
        }
    }

    private fun openMainActivity(playerName: String) {
        val intent = Intent(this@PlayerName, MainActivity::class.java)
        intent.putExtra("playerName", playerName)
        startActivity(intent)
        finish()
    }
}