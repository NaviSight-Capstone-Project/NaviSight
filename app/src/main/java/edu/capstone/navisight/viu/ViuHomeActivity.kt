package edu.capstone.navisight.viu

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import edu.capstone.navisight.R
import edu.capstone.navisight.viu.ui.camera.CameraFragment

class ViuHomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viu_home)
        supportFragmentManager.commit {
            replace(R.id.fragment_container, CameraFragment())
        }
    }
}
