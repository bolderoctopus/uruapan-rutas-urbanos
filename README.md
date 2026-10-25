# Rutas Uruapan
[![CI](https://github.com/bolderoctopus/uruapan-rutas-urbanos/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/bolderoctopus/uruapan-rutas-urbanos/actions/workflows/ci.yml)

Aplicación android para encontrar la ruta de transporte urbano más cercana en la ciudad de Uruapan, México.
Puedes descargarla en [Google Play](https://play.google.com/store/apps/details?id=com.rico.omarw.rutasuruapan).

## Contribuir
El flujo de trabajo (ramas, nombres de ramas, mensajes de commit, pull requests y releases) está documentado en [CONTRIBUTING.md](CONTRIBUTING.md).

## Antes de compilar
Para que funcione correctamente la aplicación debes contar con una API key de Google Cloud Platform con Maps SDK for Android y 
Places API habilitados.

Crea un archivo llamado *apikeys.properties* en la carpeta *RutasUruapan*.
Dentro escribe `mapsAndPlaces=KEY` sustituyendo *KEY* por tu API key.

Al hacer build en Android Studio gradle tomará de allí la API key para la insertará en el manifiesto de la aplicación.


## Change log
El historial de versiones está en [CHANGELOG.md](CHANGELOG.md).
