package FoodPassport.com

data class MealResponse(val meals: List<Meal>?)

data class Meal(
    val idMeal: String,
    val strMeal: String,
    val strMealThumb: String? = null,
    val strArea: String? = null,
    val strInstructions: String? = null,
    val strIngredient1: String? = null,
    val strIngredient2: String? = null,
    val strIngredient3: String? = null,
    val strIngredient4: String? = null,
    val strIngredient5: String? = null
)

data class CountryResponse(val meals: List<Country>?)

data class Country(val strArea: String)