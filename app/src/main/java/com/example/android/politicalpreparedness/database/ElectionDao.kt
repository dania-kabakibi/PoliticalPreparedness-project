package com.example.android.politicalpreparedness.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.android.politicalpreparedness.network.models.Election

@Dao
interface ElectionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(election: Election)

    @Query("SELECT * FROM election_table")
    fun getAllElections(): List<Election>

    @Query("SELECT * FROM election_table WHERE id= :electionId")
    fun getElection(electionId: Int): Election

    @Delete
    fun deleteElection(election: Election)

    @Query("DELETE FROM election_table")
    fun clear()

}