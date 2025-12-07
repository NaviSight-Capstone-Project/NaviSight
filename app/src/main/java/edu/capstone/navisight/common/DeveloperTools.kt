package edu.capstone.navisight.common

import android.content.Context
import android.widget.Toast

object DeveloperTools {
    fun showUnderConstruction(context: Context) {
        Toast.makeText(context,
            "Under construction, come back later \uD83D\uDE2D",
            Toast.LENGTH_LONG).show()
    }
    fun raiseError(description : String) {
        throw Exception(description)
    }
}