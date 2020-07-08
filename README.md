# Rutas Uruapan
Aplicación android para encontrar la ruta de transporte urbano más cercana en la ciudad de Uruapan, México.
Puedes descargarla en [Google Play](https://play.google.com/store/apps/details?id=com.rico.omarw.rutasuruapan).

## Antes de compilar
Para que funcione correctamente la aplicación debes contar con una API key de Google Cloud Platform con Maps SDK for Android y 
Places API habilitados.

Crea un archivo llamado *apikeys.properties* en la carpeta *RutasUruapan*.
Dentro escribe `mapsAndPlaces=KEY` sustituyendo *KEY* por tu API key.

Al hacer build en Android Studio gradle tomará de allí la API key para la insertará en el manifiesto de la aplicación.


## Change log
**1.2.0-beta _(Sin publicar)_**
- TopMargin de GoogleMapsCompass modificado.

**1.1.0-beta**
- Links para Política de Privacidad y Código Fuente agregados.
- Nombre de versión agregado en Configuración.

**1.0.1-beta**
- Cambio en el almacenamiento del API key.

**1.0.0-beta** 
- Versión inicial.
