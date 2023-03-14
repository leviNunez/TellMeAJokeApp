package com.levi.tellmeajokeapp.data.source

import com.levi.tellmeajokeapp.data.Joke
import com.levi.tellmeajokeapp.data.Result

class JokeRepositoryImpl(private val jokerRemoteDataSource: JokeDataSource) : JokeRepository {

    override suspend fun getJoke(): Result<Joke> = jokerRemoteDataSource.getJoke()

}