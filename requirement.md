# Requirement

**Functional Requirements**

* **Default user** should be able to open the app and see Posting Ads for:
  * **Category**
    * **Fashion** (Filter for Clothes, Shoes, Socks, Belt, Bags, Others)
      * Men
      * Women
      * Kids
      * Other
    * **Furniture**
      * Sofa
      * Dining
      * Decor
      * Kitchen
      * Bathroom
      * Bedroom
      * Garden
      * Other
    * **Properties** (Filter based on Rent or Sale, Type (Flats/Apartment, House/Villa, Farm), BHK, Bathroom, Furnishing)
      * Commercial
      * Residence
      * PG & Guest House
      * Lands & Plot
      * Other
    * **Electronics & Appliances** (Filter based on brand)
      * Mobile phones
      * Tablets
      * Laptops
      * Computer
      * Computer Accessories
      * Modem
      * TV
      * Fridge
      * Camera
      * Air Conditioner
      * Washing Machine
      * Kitchen Appliances
      * Other Electronics
    * **Vehicle** (Filter based on Brand, Year, Fuel, Transmission, Km, No of Owners)
      * Cars
      * Bikes
      * Scooters
      * Bicycles
      * Parts
      * Other
    * **Other**
* **Both Buyers and Sellers** can set up a profile.
* **Sellers**
  * Can post a listing with a single category.
  * Can post listings with multiple categories.
* **Buyers**
  * Can view specific listings.
  * Can view multiple listings for a single seller.
  * Can search listings based on:
    * **Location**
      * Country
      * City
      * Neighbourhood
    * **Category**
      * Search based on Sub Category
        * Sub filters
    * **Ad Title**
* **Pricing**
  * We can start with setting a standard nominal price for each listing per period (week, month, year).
  * Example: 500 per week, 1200 per month, 5000 for 6 months, 9000 per year.

**Non-functional Requirements**

* **Scalability**
  * Support for millions of active users (\~1 Million).
  * Handle millions of visits per day (50K to 1 Million).
  * Each user uploads about 10 images per listing.
  * Each image size ranges from \~2MB to 5MB.
  * Data processing volume: \~1 TB per day.
* **Performance**
  * Response time of <500ms at the 99th percentile.
  * Listing Service
