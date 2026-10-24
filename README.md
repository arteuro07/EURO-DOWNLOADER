# EURO DOWNLOADER Max

Application Android native pour télécharger des fichiers accessibles par URL HTTP/HTTPS.

## Version initiale
- Interface Android adaptative clair/sombre.
- Téléchargements via le service Android DownloadManager.
- Notifications de téléchargement.
- Enregistrement dans le dossier public Download.
- Compilation de l'APK debug via GitHub Actions.

## Compiler
Le workflow GitHub Actions compile l'application à chaque push sur `main`.
L'APK de test est publié comme artefact du workflow.

## Utilisation responsable
Utiliser uniquement des liens et contenus que vous êtes autorisé à télécharger.
Cette version ne contourne pas les DRM, les connexions, ni les restrictions d'accès.

## Limites de la première version
L'application attend une URL directe vers un fichier. La prise en charge de sites,
l'historique intégré et la file d'attente enrichie seront développés séparément.
