# EasynodeDroid

Version Android d'EasyMonerod : un nœud Monero complet (`monerod`) qui tourne directement sur le téléphone, avec démarrage/arrêt en un bouton, suivi de la synchronisation et blocage des nœuds espions.

## Obtenir l'APK (sans rien installer sur son PC)

1. Créer un dépôt sur GitHub (public ou privé) et y pousser **tout** ce dossier, y compris `.github/`.
2. Le build se lance tout seul (onglet **Actions**). Durée : ~5 min.
   - Il télécharge `monero-android-armv8-v<version>.tar.bz2` depuis getmonero.org,
   - vérifie la **signature GPG** de `hashes.txt` (clés binaryfate / luigi1111) et le **SHA256**, exactement comme `verify_monero_gpg()` du script Linux,
   - refuse de compiler si une vérification échoue.
3. Télécharger l'artefact `EasynodeDroid-monerod-<version>` en bas de la page du build, le dézipper, copier l'`.apk` sur le téléphone et l'installer (autoriser « sources inconnues »).

### Nouvelle version de monero

**Actions › Build APK › Run workflow**, laisser la case vide, **Run workflow**. La dernière version officielle est détectée, vérifiée (GPG + SHA256) et intégrée. L'APK s'installe par-dessus l'ancien : la blockchain est conservée. Pour forcer une version précise, la saisir dans la case (ex. `0.18.5.4`).

### Mises à jour pour les utilisateurs

- **Run workflow** = publication d'une *Release* `r<numéro>` avec l'APK (page publique : `github.com/<vous>/EasynodeDroid/releases/latest`). Le dépôt doit être **public** pour que les autres puissent télécharger.
- Un simple envoi de fichiers = version de test (artefact seulement), personne n'est prévenu.
- L'app interroge la dernière Release à son ouverture et toutes les 12 h quand le nœud tourne. Si elle est plus récente : bandeau « Installer la mise à jour » (un toucher) ou, si « Installer automatiquement » est actif et que le nœud tourne, mise à jour complète en arrière-plan : téléchargement, vérification que l'APK est signé avec la même clé, arrêt propre du nœud, installation, redémarrage du nœud. Android peut demander une confirmation (toujours la première fois).

### Clé de signature stable (recommandé)

Sans clé, l'APK est signé avec une clé de debug qui change à chaque build : il faut désinstaller l'app avant chaque mise à jour (et donc perdre la blockchain si elle est en mémoire interne). Pour éviter ça, créer une clé une fois :

```bash
keytool -genkeypair -v -keystore easynode.jks -alias easynode -keyalg RSA -keysize 4096 -validity 10000
base64 -w0 easynode.jks
```

puis ajouter dans **Settings › Secrets and variables › Actions** : `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` (`easynode`), `KEY_PASSWORD`.

### Compiler localement (Android Studio)

```bash
MONERO_VERSION=0.18.5.4 bash scripts/fetch_monerod.sh   # Linux / macOS / WSL
./gradlew assembleRelease
```

## Correspondance avec le script Linux

| Script `easynode_linux.sh` | Application Android |
|---|---|
| 1 · Mise à jour système, ufw, fail2ban, SSH | Sans objet : Android gère les mises à jour et l'app est isolée dans son bac à sable |
| 2 · Installer Monero CLI + vérification GPG | Fait **au build** (CI) : monerod vérifié puis embarqué dans l'APK |
| 3 · Tor (services onion SSH + RPC) | Option « Diffuser les transactions via Tor » via **Orbot** (`--tx-proxy`). Pas de service onion ni de SSH (impossible sans root) |
| 4 · bitmonero.conf + mot de passe RPC | Onglet **Réglages** (RPC local ou réseau, authentification). Blockchain toujours élaguée |
| 5 · DNS chiffrés | Réglage Android natif : *Paramètres › Réseau › DNS privé* → `dns.adguard-dns.com` |
| 6 / 7 · Disque externe / démontage | Choix **Mémoire interne / Carte SD** + bouton de suppression des données |
| 8 · Démarrage + liste d'IP bannies | Bouton **Démarrer** : liste Boog900 téléchargée et chargée via `--ban-list` |
| 9 · Arrêt (CTRL+C) | Bouton **Arrêter** ou action de la notification (arrêt propre SIGTERM) |
| 10 · Langue | Suit la langue du téléphone (français / anglais) |
| 11-12 · Infos / Notes | Onglet **Wallet** (adresse à saisir dans le wallet, identifiants, port 18080) |

## Bon à savoir

- **ARM64 uniquement** (quasiment tous les téléphones depuis 2017) : c'est la seule version Android de monerod publiée par le projet Monero.
- **Espace** : ~95 Go (blockchain toujours élaguée ; une base complète copiée est élaguée par monerod au démarrage). Une carte SD A2/U3 fonctionne mais reste plus lente.
- **Première synchro** : plusieurs heures à plusieurs jours. Laisser le téléphone branché, en Wi-Fi, et désactiver l'optimisation de batterie (bouton dans Réglages). Par défaut l'app refuse de démarrer sur données mobiles.
- **Wallet sur le téléphone** (Cake, Monerujo, Feather…) : nœud `127.0.0.1:18081`. **Wallet sur un PC du même Wi-Fi** : activer « Ouvrir le RPC au réseau local », l'adresse `IP:18081` s'affiche dans l'onglet Wallet.
- Le service tourne en premier plan avec une notification permanente : c'est obligatoire sous Android pour un processus de longue durée.

## Structure

```
app/src/main/java/org/kerlann/easynode/
  NodeService.kt    service qui lance/arrête monerod, suit la synchro (RPC get_info)
  NodeSettings.kt   réglages (équivalent bitmonero.conf)
  BanList.kt        téléchargement + nettoyage de la liste d'IP bannies
  RpcClient.kt      JSON-RPC avec authentification Digest
  Storage.kt        mémoire interne / carte SD, espace libre
  ui/Screens.kt     interface (Nœud, Réglages, Journal, Wallet)
scripts/fetch_monerod.sh   téléchargement + vérification GPG/SHA256 de monerod
.github/workflows/build.yml  build automatique de l'APK
```
