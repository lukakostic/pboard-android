package com.example.rmasapp

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.DocumentSnapshot

data class Placed(
    var id : String,
    var owner: String,
    var ownerName: String,
    var index: Int,
    var latLng: LatLng,
    var title: String,
    var description: String,
    var coins: Int,
    var image: String?, // URI local, URL server
)
{
    constructor() : this("","","",-1, LatLng(0.0,0.0),"","",0,"")
}


fun latLngToArray(latLng: LatLng): List<Double> {
    return arrayOf(latLng.latitude, latLng.longitude).toList()
}
fun arrayToLatLng(array: List<Double>): LatLng {
    return LatLng(array[0], array[1])
}

fun getPlacedFromDoc(doc: DocumentSnapshot): Placed? {
    val data = doc.data
    if (data == null) return null
    val id = data["id"] as String
    val owner = data["owner"] as String
    val ownerName = data["ownerName"] as String
    val index = (data["index"] as Long).toInt()
    val coins = (data["coins"] as Long).toInt()
    val latLngArray = data["latLng"] as? List<Double>
    val title = data["title"] as String
    val description = data["description"] as String
    val image = data["image"] as String?


    val markerLatLng = arrayToLatLng(latLngArray!!.toTypedArray().toList())

    return Placed(
        id = id,
        owner = owner,
        ownerName = ownerName,
        index = index,
        latLng = markerLatLng,
        title = title,
        description = description,
        coins = coins,
        image = image
    )
}
fun placedToMap(marker: Placed): HashMap<String, Any?> {
    return hashMapOf(
        "id" to marker.id,
        "owner" to marker.owner,
        "ownerName" to marker.ownerName,
        "index" to marker.index,
        "latLng" to latLngToArray(marker.latLng), // convert LatLng to Array<Double>
        "title" to marker.title,
        "description" to marker.description,
        "coins" to marker.coins,
        "image" to marker.image
    )
}