package com.example.rmasapp

data class Profile(
    var firstName: String,
    var lastName: String,
    var email: String,
    var phone: String,
    var password: String,
    var xp: Int,
    var coins: Int,
    var markersPlaced : Int
){
    constructor() : this("","","","","",0,0, 0)
}