package com.mrmansur.tictactoe2

import android.app.ProgressDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.os.SystemClock.setCurrentTimeMillis
import android.util.Log
import android.widget.ImageView
import com.google.firebase.database.*
import com.mrmansur.tictactoe2.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    var combinationsList = mutableListOf<Array<Int>>()
    private lateinit var progressDialog : ProgressDialog
    private lateinit var getPlayerName : String
    var playerUniqueId = "0"
    private lateinit var databaseReference: DatabaseReference
    private var opponentFound = false
    private var opponentUniqueId = "0"
    var status = "matching"
    private var playerTurn = ""
    var connectionId = ""
    private lateinit var wonEventListener : ValueEventListener
    private lateinit var turnsEventListener : ValueEventListener
    private var doneBoxes = ArrayList<String>()
    private var boxesSelectedBy = arrayListOf("","","","","","","","","")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fillCombinationsList()
        createProgressDialog()
        playerUniqueId = System.currentTimeMillis().toString()
        getPlayerName = intent.getStringExtra("playerName").toString()
        binding.player1TV.text = getPlayerName
        setDatabaseReference()
        initViews()
    }

    private fun initViews() {

        binding.apply {
            image1.setOnClickListener {
                playGame(image1, "1")
            }

            image2.setOnClickListener {
                playGame(image2, "2")
            }

            image3.setOnClickListener {
                playGame(image3, "3")
            }

            image4.setOnClickListener {
                playGame(image4, "4")
            }

            image5.setOnClickListener {
                playGame(image5, "5")
            }

            image6.setOnClickListener {
                playGame(image6, "6")
            }

            image7.setOnClickListener {
                playGame(image7, "7")
            }

            image8.setOnClickListener {
                playGame(image8, "8")
            }

            image9.setOnClickListener {
                playGame(image9, "9")
            }
        }
    }

    private fun playGame(imageView: ImageView, index : String){
        if (!doneBoxes.contains(index) && playerTurn == playerUniqueId){
            imageView.setImageResource(R.drawable.img_x)

            databaseReference.child("turns").child(connectionId).child((doneBoxes.size + 1).toString()).child("box_position").setValue(index)
            databaseReference.child("turns").child(connectionId).child((doneBoxes.size + 1).toString()).child("player_id").setValue(playerUniqueId)

            playerTurn = opponentUniqueId
        }
    }

    private fun setDatabaseReference() {
        databaseReference = FirebaseDatabase.getInstance().getReferenceFromUrl("https://tictactoe-fa7db-default-rtdb.firebaseio.com/")

        databaseReference.child("connections").addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!opponentFound){

                    //checking if there are others in the firebase realtime database
                    if (snapshot.hasChildren()){

                        //checking all connections if other user are waiting for a user to play the match
                        for (connections in snapshot.children){

                            //getting connection uniqueId
                            val conId = connections.key.toString()

                            //2 players are required to play a  game
                            //If getPlayersCount is 1 it means other player is waiting for a opponent  to play the game
                            //else if getPlayersCount is 2 it means this connection has completed with 2 players
                            val getPlayersCount = connections.childrenCount.toInt()

                            //after created a new connection waiting for other to join
                            if (status == "waiting"){

                                //if getPlayersCount is 2 means other player joined the match
                                if( getPlayersCount == 2){
                                    playerTurn = playerUniqueId
                                    applyPlayerTurn(playerTurn)

                                    //true when player found in connections
                                    var playerFound = false

                                    //getting players in connection
                                    for (players in connections.children){
                                        val getPlayerUniqueId = players.key

                                        //check if player id match with user who created connection(this user). If match then get opponent details
                                        if (getPlayerUniqueId == playerUniqueId){
                                            playerFound = true
                                        }else if (playerFound){
                                            val getOpponentPlayerName = players.child("player_name").getValue(String::class.java)
                                            opponentUniqueId = players.key!!

                                            // set opponent player name to the TextView
                                            binding.player2TV.text = getOpponentPlayerName

                                            // assigning connection id
                                            connectionId = conId
                                            opponentFound = true

                                            //adding turns listener and won listener to the database reference.
                                            databaseReference.child("won").child(connectionId).addValueEventListener(wonEventListener)
                                            databaseReference.child("turns").child(connectionId).addValueEventListener(turnsEventListener)

                                            //hide progress dialog if showing
                                            if (progressDialog.isShowing){
                                                progressDialog.dismiss()
                                            }

                                            //once the connection has made remove connectionListener from Database Reference
                                            databaseReference.child("connections").removeEventListener(this)
                                        }
                                    }
                                }
                            }
                            // in case user has not created the connection/room because of other rooms are available to join
                            else{
                                //checking if the connection has 1 player and need 1 player to play the match then join this connection
                                if (getPlayersCount == 1){
                                    //add player to the connection
                                    connections.child(playerUniqueId).child("player_name").ref.setValue(getPlayerName)

                                    //getting both player
                                    for (players in connections.children){
                                        val getOpponentName = players.child("player_name").getValue(String::class.java)
                                        opponentUniqueId = players.key!!

                                        //first turn will be of who created the connection/room
                                        playerTurn = opponentUniqueId
                                        applyPlayerTurn(playerTurn)

                                        //setting playerName to the TextView
                                        binding.player2TV.text = getOpponentName

                                        //assigning connection id
                                        connectionId = conId
                                        opponentFound = true

                                        //adding turns listener and won listener to the database reference.
                                        databaseReference.child("turns").child(connectionId).addValueEventListener(turnsEventListener)
                                        databaseReference.child("won").child(connectionId).addValueEventListener(wonEventListener)

                                        //hide progress dialog if showing
                                        if (progressDialog.isShowing){
                                            progressDialog.dismiss()
                                        }

                                        //once the connection has made remove connectionListener from Database Reference
                                        databaseReference.child("connections").removeEventListener(this)

                                        break
                                    }
                                }
                            }
                        }

                        // check if opponent is not found and user is not waiting for the opponent anymore then create a new connection
                        if (!opponentFound && !status.equals("waiting")){
                            //generating unique id for the connection
                            val connectionUniqueId = System.currentTimeMillis().toString()
                            // adding first player to the connection and writing for other to complete the connection and play the game
                            snapshot.child(connectionUniqueId).child(playerUniqueId).child("player_name").ref.setValue(getPlayerName)

                            status = "waiting"
                        }
                    }
                    //if there is no connection available in the firebase database then create a new connection
                    // it is like creating a room and waiting for other players to join the room
                    else{
                        //generating unique id for the connection
                        val connectionUniqueId = System.currentTimeMillis().toString()
                        // adding first player to the connection and writing for other to complete the connection and play the game
                        snapshot.child(connectionUniqueId).child(playerUniqueId).child("player_name").ref.setValue(getPlayerName)

                        status = "waiting"
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })

        wonEventListener = object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("TAG", "onDataChange1: ")
                if(snapshot.hasChild("player_id")){
                    Log.d("TAG", "onDataChange2: ")
                    val getWinPlayerId = snapshot.child("player_id").getValue(String::class.java)

                    Log.d("TAG", "getWinPlayerId: $getWinPlayerId")
                    Log.d("TAG", "playerUniqueId: $playerUniqueId")

                    val winDialog : WinDialog = if (getWinPlayerId == playerUniqueId){
                        WinDialog(this@MainActivity, "You won the game")
                    }else{
                        WinDialog(this@MainActivity, "Opponent won the game")
                    }
                    winDialog.setCancelable(false)
                    winDialog.show()

                    databaseReference.child("turns").child(connectionId).removeEventListener(turnsEventListener)
                    databaseReference.child("won").child(connectionId).removeEventListener(wonEventListener)

                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        }

        turnsEventListener = object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                //getting all return
                for (dataSnapshot in snapshot.children){
                    if (dataSnapshot.childrenCount == 2L){
                        //getting box position selected by the user
                        val getBoxPosition = (dataSnapshot.child("box_position").getValue(String::class.java))!!.toInt()
                        //getting player id who selected the box
                        val getPlayerId = dataSnapshot.child("player_id").getValue(String::class.java)!!

                        //checking if user has not selected the box before
                        if (!doneBoxes.contains(getBoxPosition.toString())){
                            //select the box
                            doneBoxes.add(getBoxPosition.toString())

                            when (getBoxPosition) {
                                1 -> {
                                    selectBox(binding.image1, getBoxPosition, getPlayerId)
                                }
                                2 -> {
                                    selectBox(binding.image2, getBoxPosition, getPlayerId)
                                }
                                3 -> {
                                    selectBox(binding.image3, getBoxPosition, getPlayerId)
                                }
                                4 -> {
                                    selectBox(binding.image4, getBoxPosition, getPlayerId)
                                }
                                5 -> {
                                    selectBox(binding.image5, getBoxPosition, getPlayerId)
                                }
                                6 -> {
                                    selectBox(binding.image6, getBoxPosition, getPlayerId)
                                }
                                7 -> {
                                    selectBox(binding.image7, getBoxPosition, getPlayerId)

                                }
                                8 -> {
                                    selectBox(binding.image8, getBoxPosition, getPlayerId)

                                }
                                9 -> {
                                    selectBox(binding.image9, getBoxPosition, getPlayerId)
                                }
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        }
    }

    private fun applyPlayerTurn(playerTurn: String) {
        if (playerTurn == playerUniqueId){
            binding.player1Layout.setBackgroundResource(R.drawable.round_back_player_with_stroke)
            binding.player2Layout.setBackgroundResource(R.drawable.round_back_player)
        }else{
            binding.player2Layout.setBackgroundResource(R.drawable.round_back_player_with_stroke)
            binding.player1Layout.setBackgroundResource(R.drawable.round_back_player)
        }
    }

    private fun selectBox(imageView: ImageView, selectedBoxPosition : Int, selectedByPlayer : String){
        boxesSelectedBy[selectedBoxPosition - 1] = selectedByPlayer

        playerTurn = if (selectedByPlayer == playerUniqueId){
            imageView.setImageResource(R.drawable.img_x)
            opponentUniqueId
        }else{
            imageView.setImageResource(R.drawable.circle_png)
            playerUniqueId
        }

        applyPlayerTurn(playerTurn)

        if (checkPlayerWin(selectedByPlayer)){
            databaseReference.child("won").child("player_id").setValue(selectedByPlayer)
            val winDialog = WinDialog(this, "Game is over!")
            winDialog.setCancelable(false)
            winDialog.show()
        }

        if (doneBoxes.size == 9){
            val winDialog = WinDialog(this, "It is a Draw!")
            winDialog.setCancelable(false)
            winDialog.show()
        }

    }

    private fun checkPlayerWin(playerId : String) : Boolean{
        var isPlayerWon = false
        for (i in 0 until combinationsList.size){

            val combination = combinationsList[i]

            if (boxesSelectedBy[combination[0]] == playerId &&
                boxesSelectedBy[combination[1]] == playerId &&
                boxesSelectedBy[combination[2]] == playerId){
                isPlayerWon = true
            }
        }
        return isPlayerWon
    }

    private fun fillCombinationsList(){
        combinationsList.add(arrayOf(0,1,2))
        combinationsList.add(arrayOf(3,4,5))
        combinationsList.add(arrayOf(6,7,8))
        combinationsList.add(arrayOf(0,3,6))
        combinationsList.add(arrayOf(1,4,7))
        combinationsList.add(arrayOf(2,5,8))
        combinationsList.add(arrayOf(2,4,6))
        combinationsList.add(arrayOf(0,4,8))
    }

    private fun createProgressDialog(){
        progressDialog = ProgressDialog(this)
        progressDialog.setCancelable(false)
        progressDialog.setMessage("Waiting for Opponent")
        progressDialog.show()
    }
}