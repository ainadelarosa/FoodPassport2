package FoodPassport.com

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

interface CountryInfoApi {
    @GET("name/{name}?fullText=true")
    suspend fun getCountryInfo(@Path("name") name: String): List<CountryInfo>
}

data class CountryInfo(
    val name: CountryName,
    val capital: List<String>?,
    val population: Long,
    val region: String,
    val subregion: String?,
    val languages: Map<String, String>?,
    val currencies: Map<String, Currency>?,
    val flags: Flags,
    val area: Double?
)

data class CountryName(val common: String, val official: String)
data class Currency(val name: String, val symbol: String?)
data class Flags(val png: String?, val svg: String?)