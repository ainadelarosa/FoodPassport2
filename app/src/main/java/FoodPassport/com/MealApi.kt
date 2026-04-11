package FoodPassport.com

import retrofit2.http.GET
import retrofit2.http.Query

interface MealApi {
    @GET("filter.php")
    suspend fun getRecipesByCountry(@Query("a") country: String): MealResponse

    @GET("list.php")
    suspend fun getCountries(@Query("a") list: String = "list"): CountryResponse

    @GET("lookup.php")
    suspend fun getMealDetail(@Query("i") id: String): MealResponse
}