package com.example.whereami

class NeshanResponse (
    val status: String,
    val formatted_address: String,
    val route_name: String,
    val neighbourhood: String,
    val city: String,
    val state: String,
    val municipality_zone: String,
    val in_traffic_zone: String,
    val in_odd_even_zone: String
)
