package com.blackclaw.android.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.car.app.model.GridItem
import androidx.car.app.model.GridTemplate
import androidx.car.app.model.ItemList
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import com.blackclaw.android.R

/**
 * Android Auto home: a driving-safe grid of one-tap actions. "Preguntar",
 * "Navigate", "Music" and "Call" open a voice search (the car transcribes
 * the speech); "Notifications" runs immediately and reads the result aloud.
 *
 * Every action funnels into the same task pipeline the phone uses, so anything
 * BlaqClaw can do by voice on the phone also works here.
 */
class BlaqClawHomeScreen(carContext: CarContext) : Screen(carContext) {

    private fun icon(resId: Int): CarIcon =
        CarIcon.Builder(IconCompat.createWithResource(carContext, resId)).build()

    private fun gridItem(title: String, resId: Int, onClick: () -> Unit): GridItem =
        GridItem.Builder()
            .setTitle(title)
            .setImage(icon(resId), GridItem.IMAGE_TYPE_ICON)
            .setOnClickListener(onClick)
            .build()

    override fun onGetTemplate(): Template {
        val list = ItemList.Builder()
            .addItem(gridItem("Preguntar", R.drawable.ic_car_mic) {
                screenManager.push(CarAskScreen(carContext, hint = "Ask me anything", commandPrefix = ""))
            })
            .addItem(gridItem("Navigate", R.drawable.ic_car_navigation) {
                screenManager.push(CarAskScreen(carContext, hint = "Where are we going?", commandPrefix = "navigate to "))
            })
            .addItem(gridItem("Music", R.drawable.ic_car_music) {
                screenManager.push(CarAskScreen(carContext, hint = "What should I play?", commandPrefix = "play "))
            })
            .addItem(gridItem("Call", R.drawable.ic_car_call) {
                screenManager.push(CarAskScreen(carContext, hint = "Who should I call?", commandPrefix = "call "))
            })
            .addItem(gridItem("Notifications", R.drawable.ic_car_bell) {
                screenManager.push(CarTaskScreen(carContext, "read my notifications without opening apps"))
            })
            .build()

        return GridTemplate.Builder()
            .setTitle("BlaqClaw")
            .setHeaderAction(Action.APP_ICON)
            .setSingleList(list)
            .build()
    }
}
