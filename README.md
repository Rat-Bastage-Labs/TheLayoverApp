#### The Layover App
![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![JavaFX](https://img.shields.io/badge/javafx-%23FF0000.svg?style=for-the-badge&logo=javafx&logoColor=white)
![JSON](https://img.shields.io/badge/json-5E5C5C?style=for-the-badge&logo=json&logoColor=white)<br>
![Replit](https://img.shields.io/badge/Replit-DD1200?style=for-the-badge&logo=Replit&logoColor=white)
![ChatGPT](https://img.shields.io/badge/chatGPT-74aa9c?style=for-the-badge&logo=openai&logoColor=white)
![HTML](https://img.shields.io/badge/HTML-239120?style=for-the-badge&logo=html5&logoColor=white)
---
##### A JavaFX-based social travel application designed to help travelers connect, explore airport information, and discover real-time flight data during layovers.
---
#### <ins>Overview:</ins> 

##### The Layover App enhances the airport experience by combining:
##### - Social interaction between travelers
##### - Real-time flight tracking
##### - Global airport exploration
##### - Interactive “quick match” meetup system

##### Built with JavaFX, the app provides a modern UI and a unique swipe-based connection experience for users during layovers.
---
#### <ins>Features:</ins>
##### - Social interaction between travelers
##### - Smart Navigation
##### - Central “Find” hub for:
##### - People
##### - Flights
##### - Airports
##### -  Scroll + click interaction for quick switching
---
#### <ins>Meet Travelers:</ins>
##### - Swipe-style matching system (like Tinder)
##### - Randomized traveler profiles
##### - Match rewards system (+1 or +2 meetups)
##### - Profile overlays with animations
##### - Match history tracking
---
#### <ins>Profile System:</ins> 
##### - Editable user profile:
##### - Display name
##### - Username
##### - Bio
##### - Age range & gender
##### - Email verification indicator
##### - Persistent local storage (JSON)
##### - Match history with:
##### - Unmatch option
##### - Report/block system
---
#### <ins>Airport Explorer:</ins>
##### - Loads global airport data via CSV
##### - Sortable columns:
##### - Name
##### - IATA code
##### - City
##### - Country
##### - Continent
##### <ins>Clickable:</ins>
##### - Airport websites
##### - Google Maps locations
---
#### <ins>Flight Tracking:</ins> 
##### - Real-time arrivals & departures
##### - Filters by:
##### - Airline whitelist
##### - Time window (±24 hours)
##### - Displays:
##### - Flight number
##### - Airline
##### - Status
##### - Gate & terminal
##### - Toggle between arrivals/departures
---
#### <ins>Location-Based Features:</ins> 
##### - Local HTTP server (port 8080)
##### - Receives browser geolocation
##### - Calculates nearest airport using Haversine formula
---
#### <ins>Lobby System:</ins> 
##### - Activity categories like:
##### - Chill & Chat
##### - Food & Drink
##### - Games
##### - Meetups
##### - Interactive cards with animations
##### - Quick match entry points
---
#### <ins>Safety & Legal:</ins> 
##### - Required agreement before meeting users:
##### - Age confirmation (18+)
##### - Terms acknowledgment
##### - Built-in legal disclaimer page
##### - Report & block functionality
--- 
#### <ins>Tech Stack:</ins> 
##### - Java 17+
##### - JavaFX
##### - Gson (JSON handling)
##### - HttpServer (local backend)
##### - AviationStack API (flight data)
##### - CSV parsing (airport dataset)
---
#### <ins>Environment Variables</ins>

##### Set the following before running:
```
AVIATIONSTACK_KEY=your_api_key
AVIATIONSTACK_URL=http://api.aviationstack.com/v1/flights
AIRPORTS_CSV_URL=https://davidmegginson.github.io/ourairports-data/airports.csv
```
---
#### <ins>Running the App</ins>
##### - 1. Clone the repo
```
git clone https://github.com/inglorious-ratbastard/TheLayoverApp.git
cd layover-app
```
##### - 2. Compile & Run
```
javac Main.java
java Main
```
##### - 3. Enable Location (Optional)
##### Open index.html served at:
```
http://localhost:8080 
```
##### Allows location access to enable nearest airport detection
--- 
##### <ins>Disclaimer</ins>
##### This app is a social coordination tool only.
##### - No identity verification is performed
##### - Users meet at their own risk
##### - Not affiliated with airports, airlines, or government agencies
---
##### <ins>Author</ins>
##### Javier Yzaguirre
##### © 2026 All Rights Reserved
##### Unauthorized copying or distribution is prohibited. 
---
