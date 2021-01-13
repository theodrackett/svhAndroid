package com.streetvendorhelper.util

import android.content.Context
import java.util.*
import com.streetvendorhelper.R
import com.streetvendorhelper.model.Event


object VendorUtils {

    fun getCategoryIdentifier(categoryName: String): String {
        var result = "df"
        if (categoryName.toLowerCase().contains("art") || categoryName.toLowerCase().contains("wine")){
            result = "aw"
        } else if (categoryName.toLowerCase().contains("concert")){
            result = "aw"
        } else if (categoryName.toLowerCase().contains("food")){
            result = "fi"
        } else if (categoryName.toLowerCase().contains("shopping")){
            result = "si"
        }

        return result
    }

    fun getRandomImage(context: Context, category: String): Int {
        val categoryId = getCategoryIdentifier(category)
        if (categoryId == "df") {
            return R.drawable.df
        }
        val no = Random().nextInt(9) + 1
        return getResId(context, categoryId+no)
    }

    fun getResId(context: Context, resName: String): Int {
        return context.resources.getIdentifier(resName, "drawable", context.packageName)
    }

    fun calculateRating(item: Event): Float{
        if (item.Reviews.isEmpty()){
            return 0.0f
        }

        var total: Long = 0
        for ((key, value) in item.Reviews) {
            total += value.Rating
        }

        return (total/ item.Reviews.count()).toFloat()
    }


}
