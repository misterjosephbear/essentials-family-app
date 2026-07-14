package com.isaacshub.app.routehelper.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [RouteHelperRouteEntity::class, CandidateAddressEntity::class, RoutedStopEntity::class],
    version = 1,
    exportSchema = true
)
abstract class RouteHelperDatabase : RoomDatabase() {

    abstract fun routeHelperDao(): RouteHelperDao

    companion object {
        @Volatile private var instance: RouteHelperDatabase? = null

        fun getInstance(context: Context): RouteHelperDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RouteHelperDatabase::class.java,
                    "route_helper.db"
                )
                    // Same rule as every other database in this app: routes/stops are real user
                    // data - any future schema change needs an explicit Migration, never a
                    // destructive fallback.
                    .build().also { instance = it }
            }
    }
}
