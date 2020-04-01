# Rutas Uruapan
Aplicación android para encontrar la ruta de transporte urbano más cercana en la ciudad de Uruapan, México.

## Antes de compilar
Para que funcione correctamente la aplicación debes contar con una API key de Google Cloud Platform con Maps SDK for Android y 
Places API habilitados.

Crea un archivo llamado *apikeys.properties* en la carpeta *RutasUruapan*.
Dentro escribe `mapsAndPlaces=KEY` sustituyendo *KEY* por tu API key.

Al hacer build en Android Studio gradle tomará de allí la API key para la insertará en el manifiesto de la aplicación.
