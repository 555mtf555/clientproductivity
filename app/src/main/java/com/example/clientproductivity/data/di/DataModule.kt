package com.example.clientproductivity.data.di

import android.content.Context
import androidx.room.Room
import com.example.clientproductivity.data.AppDatabase
import com.example.clientproductivity.data.ClientRepository
import com.example.clientproductivity.data.TaskRepository
import com.example.clientproductivity.data.dao.ActivityLogDao
import com.example.clientproductivity.data.dao.ClientDao
import com.example.clientproductivity.data.dao.ProjectDao
import com.example.clientproductivity.data.dao.TaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "client_productivity.db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideClientDao(database: AppDatabase): ClientDao = database.clientDao()

    @Provides
    fun provideProjectDao(database: AppDatabase): ProjectDao = database.projectDao()

    @Provides
    fun provideTaskDao(database: AppDatabase): TaskDao = database.taskDao()

    @Provides
    fun provideActivityLogDao(database: AppDatabase): ActivityLogDao = database.activityLogDao()

    @Provides
    @Singleton
    fun provideTaskRepository(
        database: AppDatabase,
        clientDao: ClientDao,
        projectDao: ProjectDao,
        taskDao: TaskDao,
        activityLogDao: ActivityLogDao
    ): TaskRepository {
        return TaskRepository(database, clientDao, projectDao, taskDao, activityLogDao)
    }

    @Provides
    @Singleton
    fun provideClientRepository(
        clientDao: ClientDao,
        projectDao: ProjectDao
    ): ClientRepository {
        return ClientRepository(clientDao, projectDao)
    }
}
