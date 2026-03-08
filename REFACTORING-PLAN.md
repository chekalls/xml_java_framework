# Plan Technique De Refactoring Du Framework

## Objectif

Ce document met a jour le plan de refactoring en tenant compte de l'etat reel du projet apres les deplacements de packages que vous avez deja commences.

L'objectif n'est plus seulement de decrire une architecture cible, mais de fournir une feuille de route technique realiste a partir du code actuel.

Ce plan poursuit 4 objectifs :

1. stabiliser les deplacements deja faits
2. terminer l'extraction des responsabilites encore concentrees dans `MethodManager` et `FrontControllerServlet`
3. clarifier l'architecture interne avant toute migration multi-module Maven
4. reduire le couplage entre la couche web, le coeur du framework et le systeme de services

---

## Etat Actuel Observe Dans Le Code

### Deplacements deja effectues

Les elements suivants ont deja ete deplaces avec succes :

- `MethodManager` vers `mg.miniframework.core.invocation`
- `FrontControllerServlet` vers `mg.miniframework.web.servlet`
- `CachedService` vers `mg.miniframework.service.registry`
- `FrameworkService` vers `mg.miniframework.service.api`
- `RouteMap` vers `mg.miniframework.core.routing`
- `CachedMethodInfo` vers `mg.miniframework.core.routing`
- `Url` vers `mg.miniframework.core.routing`
- `RoutePatternUtils` vers `mg.miniframework.core.routing`
- `ConfigLoader` vers `mg.miniframework.core.config`
- `ContentRenderManager` vers `mg.miniframework.web.response`
- `MetricsManager` vers `mg.miniframework.metrics`

### Evolutions deja realisees dans la logique

- `DataTypeUtils.getClassFieldWithAnnotation(...)` est implemente
- l'injection de service dans `MethodManager` a deja ete adaptee pour eviter le `ClassCastException` quand un service n'herite pas de `FrameworkService`

### Mise à jour après Phase 2 (détails)

La Phase 2 d'extraction du coeur de `MethodManager` a été réalisée. Voici le bilan précis et les artefacts créés/modifiés — utile pour les revues de code et pour valider la compilation :

- Fichiers nouveaux ajoutés / responsibilities extraites :
  - `mg.miniframework.service.injection.ServiceInjector` — encapsule la logique d'injection des champs annotés `@Service`.
    - Fichier: `src/main/java/mg/miniframework/service/injection/ServiceInjector.java`
  - `mg.miniframework.core.binding.RequestObjectBinder` — extrait `getObjectInstanceFromRequest(...)` et le binding récursif d'objets.
    - Fichier: `src/main/java/mg/miniframework/core/binding/RequestObjectBinder.java`
  - `mg.miniframework.web.multipart.MultipartRequestReader` + `MultipartRequestData` — centralise la lecture des `Part` et la création d'objets fichiers.
    - Fichiers: `src/main/java/mg/miniframework/web/multipart/MultipartRequestReader.java`, `.../MultipartRequestData.java`
  - `mg.miniframework.core.invocation.MethodArgumentResolver` — extrait la logique de résolution des arguments (`paramInfos`) et retourne un `ResolveResult`.
    - Fichier: `src/main/java/mg/miniframework/core/invocation/MethodArgumentResolver.java`
  - `mg.miniframework.core.invocation.ControllerMethodInvoker` — renommage / remaniement de `MethodManager` pour n'être plus qu'un orchestrateur.
    - Fichier: `src/main/java/mg/miniframework/core/invocation/ControllerMethodInvoker.java`

- Fichiers modifiés (références mises à jour) :
  - `FrontControllerServlet` : utilise désormais `ControllerMethodInvoker` au lieu de `MethodManager`.
    - Fichier: `src/main/java/mg/miniframework/web/servlet/FrontControllerServlet.java`

- Comportement conservé :
  - Le flux d'exécution reste identique : création du controller, injection des services, résolution des arguments, invocation de la méthode.
  - Les méthodes utilitaires (encodage request, lecture de Part) sont conservées dans l'orchestrateur afin d'assurer la rétro-compatibilité pour l'instant.

- Pièges connus / points d'attention :
  - `LogManager` package (`mg.miniframework.loggin`) n'a pas encore été renommé : renommer avant d'étendre ces refactors pour éviter dettes techniques.
  - Certaines classes déplacées peuvent générer des imports inutilisés — une passe `mvnw -DskipTests package` fera remonter les warnings à corriger.
  - `CachedService` reste la forme actuelle du registre de services ; prévoir un adaptateur si vous changez l'API `ServiceRegistry` plus tard.

---


### Incoherences transitoires encore presentes

#### 1. Package `loggin`

`LogManager` est actuellement dans `mg.miniframework.loggin`.

Le package cible doit etre :

```text
mg.miniframework.logging
```

Ce renommage doit etre fait rapidement pour eviter de figer une faute de nommage dans tout le projet.

#### 2. Refactoring de packages avance, refactoring de responsabilites encore incomplet

Aujourd'hui :

- les fichiers ont deja commence a etre mieux ranges
- mais `MethodManager` reste encore tres charge
- `FrontControllerServlet` reste encore trop central
- `DataTypeUtils` reste encore une classe utilitaire trop large

Conclusion :

la bonne suite n'est pas de refaire la structure globale une nouvelle fois, mais de terminer le refactoring logique a l'interieur de la structure actuelle.

---

## Decision D'Architecture Recommandee Maintenant

Vu ce qui a deja ete fait, il est recommande de conserver pour l'instant une architecture en un seul module Maven, mais organisee par packages stables.

La strategie recommandee est donc :

1. stabiliser les packages deja deplaces
2. extraire les composants depuis les grosses classes
3. nettoyer les utilitaires transverses
4. clarifier le registre de services
5. seulement ensuite envisager une migration multi-module

---

## Structure Package Recommandee A Court Terme

Structure cible conseillee a partir de votre etat actuel :

```text
mg.miniframework
  annotation/
  core/
    binding/
    config/
    conversion/
    invocation/
    reflection/
    routing/
    scanning/
  web/
    multipart/
    response/
    servlet/
  service/
    api/
    injection/
    lifecycle/
    registry/
  security/
  metrics/
  logging/
  exceptions/
  persistence/
  ui/
```

### Choix a figer

`ConfigLoader` est deja dans `mg.miniframework.core.config`.

Il est recommande de garder ce choix plutot que de le redeplacer encore vers `mg.miniframework.config`, afin de limiter les mouvements inutiles.

---

## Etat D'Avancement Par Zone

### 1. Packages

Statut : partiellement termine

#### Deja en place

- `core.invocation`
- `core.routing`
- `core.config`
- `web.servlet`
- `web.response`
- `service.api`
- `service.registry`
- `metrics`

#### A creer ou finaliser

- `core.binding`
- `core.conversion`
- `core.reflection`
- `core.scanning`
- `web.multipart`
- `service.injection`
- `service.lifecycle`
- `logging`
- `exceptions`

### 2. `MethodManager`

Statut : deplace, mais encore monolithique

Ce qui est deja fait :

- la classe est dans `mg.miniframework.core.invocation`
- la logique d'injection `@Service` est plus robuste qu'avant

Ce qui reste a faire :

- extraire la logique d'injection des services
- extraire le binding d'objet depuis la requete
- extraire la gestion multipart
- extraire la resolution des arguments de methode
- reduire la classe a un orchestrateur simple

### 3. `FrontControllerServlet`

Statut : deplace, mais encore trop charge

Ce qui est deja fait :

- la classe est dans `mg.miniframework.web.servlet`
- elle utilise deja plusieurs classes relocalisees

Ce qui reste a faire :

- extraire l'initialisation du framework
- extraire le scan des controllers
- extraire la construction des routes
- extraire le dispatch de requetes
- extraire le rendu des erreurs HTTP

### 4. `DataTypeUtils`

Statut : toujours centralisee

Ce qui est deja fait :

- `getClassFieldWithAnnotation(...)` est implemente

Ce qui reste a faire :

- la decomposer par responsabilites
- migrer progressivement les appels
- garder si besoin une facade temporaire pendant la transition

### 5. `CachedService`

Statut : package deplace, design encore transitoire

Ce qui est deja fait :

- le registre est dans `service.registry`
- `FrameworkService` est deja dans `service.api`

Ce qui reste a faire :

- clarifier le contrat de service
- definir une API de registre plus propre
- distinguer proprement les services simples et les services avec cycle de vie

---

## Priorites Reelles A Partir De Maintenant

### Phase 1 - Stabilisation immediate

Avant toute nouvelle extraction, faire ces corrections :

1. renommer `mg.miniframework.loggin` en `mg.miniframework.logging`
2. corriger tous les imports lies a `LogManager`
3. nettoyer les imports inutiles sur les classes deja deplacees
4. verifier que tout compile apres ce nettoyage

### Phase 2 - Extraire le coeur de `MethodManager`

Ordre recommande :

1. extraire `ServiceInjector`
2. extraire `RequestObjectBinder`
3. extraire `MultipartRequestReader`
4. extraire `MethodArgumentResolver`
5. laisser `MethodManager` uniquement comme orchestrateur
6. renommer ensuite `MethodManager` en `ControllerMethodInvoker`

### Phase 3 - Nettoyer `DataTypeUtils`

1. creer `TypeConversionUtils`
2. creer `CollectionTypeUtils`
3. creer `MapTypeResolver`
4. creer `AnnotatedFieldUtils`
5. migrer les appels progressivement
6. garder `DataTypeUtils` comme facade temporaire le temps de la migration

### Phase 4 - Simplifier `FrontControllerServlet`

1. extraire `FrameworkBootstrap`
2. extraire `ControllerScanner`
3. extraire `RouteRegistryBuilder`
4. extraire `RequestDispatcherEngine`
5. extraire `ExceptionResolver`
6. extraire `NotFoundResponseRenderer`

### Phase 5 - Refondre le registre de services

1. choisir entre modele strict et modele flexible
2. introduire une abstraction `ServiceRegistry`
3. faire de `CachedService` une implementation transitoire ou le renommer
4. introduire un cycle de vie optionnel pour les services initialisables

### Phase 6 - Eventuelle evolution multi-module

Ne faire cette etape qu'apres stabilisation du refactoring logique.

---

## Ce Qu'Il Faut Faire Techniquement, Fichier Par Fichier

### 1. `LogManager`

Action : corriger le package

Ce qu'il faut faire :

1. deplacer `LogManager` de `mg.miniframework.loggin` vers `mg.miniframework.logging`
2. corriger tous les imports dans les classes qui l'utilisent
3. verifier qu'aucune nouvelle classe n'importe encore `mg.miniframework.loggin.*`

Pourquoi cette action est prioritaire :

parce que c'est une incoherence de structure qui va sinon se propager dans toutes les futures extractions.

### 2. `MethodManager`

Action : decoupage progressif

Ce que vous devez retirer de la classe :

- la boucle d'injection des champs annotes `@Service`
- `getObjectInstanceFromRequest(...)`
- `getRequestEncoding(...)`
- `readPartValue(...)`
- `readPartBytes(...)`
- toute la boucle de resolution des `paramInfos`

Ce que la classe doit garder a la fin :

- creation de l'instance du controller
- appel a `ServiceInjector`
- appel a `MethodArgumentResolver`
- invocation finale de la methode cible

Nom final recommande :

```text
mg.miniframework.core.invocation.ControllerMethodInvoker
```

### 3. `DataTypeUtils`

Action : decomposition en utilitaires specialises

#### A extraire vers `mg.miniframework.core.conversion.TypeConversionUtils`

- `convertParam(...)`
- `convertListToTargetType(...)`
- `convertElement(...)`
- `isPrimitiveOrWrapper(...)`

#### A extraire vers `mg.miniframework.core.reflection.CollectionTypeUtils`

- `isArrayType(...)`
- `isListType(...)`
- `getContentType(Class<?>)`
- `getContentType(Field)`

#### A extraire vers `mg.miniframework.core.binding.MapTypeResolver`

- `resolveMapForParameter(...)`
- `isMapOfType(...)`

#### A extraire vers `mg.miniframework.core.reflection.AnnotatedFieldUtils`

- `getClassFieldWithAnnotation(...)`

### 4. `CachedService`

Action : preparer un vrai registre de services

Le nom `CachedService` ne correspond plus tout a fait au role vise. Cette classe devient en pratique un registre d'instances par controller.

Ce qu'il faut faire :

1. introduire une interface `ServiceRegistry`
2. faire de `CachedService` une implementation concrete transitoire, ou la renommer en `DefaultServiceRegistry`
3. encapsuler l'acces direct a la map dans une API explicite

API cible recommandee :

```java
public interface ServiceRegistry {
    <T> T getService(Class<?> ownerClass, Class<T> serviceType) throws Exception;
    <T> void registerService(Class<?> ownerClass, Class<T> serviceType, T instance);
}
```

### 5. `FrontControllerServlet`

Action : transformation en facade HTTP fine

Ce qu'il faut sortir de la classe :

- l'initialisation globale des composants
- le scan des controllers
- la construction du registre de routes
- le dispatch des requetes
- le rendu 404
- la resolution des exceptions

Ce que la classe doit garder a la fin :

- l'integration Servlet API
- la delegation vers les composants du framework

---

## Nouvelles Classes A Creer

### A creer en premier

#### `mg.miniframework.service.injection.ServiceInjector`

Responsabilite :

- injecter les champs annotes `@Service` dans un controller

Code a deplacer depuis `MethodManager` :

- la boucle sur `controllerFields`
- la resolution des instances de service

API recommandee :

```java
public class ServiceInjector {
    public void injectServices(Object controllerInstance, CachedService cachedService) throws Exception {
    }
}
```

#### `mg.miniframework.core.binding.RequestObjectBinder`

Responsabilite :

- construire un objet complexe a partir de `HttpServletRequest`

Code a deplacer :

- `getObjectInstanceFromRequest(...)`

API recommandee :

```java
public class RequestObjectBinder {
    public Object bind(Class<?> targetType, HttpServletRequest request, String prefix) throws Exception {
    }
}
```

#### `mg.miniframework.web.multipart.MultipartRequestReader`

Responsabilite :

- lire une requete multipart et la transformer en donnees exploitables

Code a deplacer :

- `getRequestEncoding(...)`
- `readPartValue(...)`
- `readPartBytes(...)`
- la logique de traitement multipart

Objet complementaire a creer :

- `MultipartRequestData`

#### `mg.miniframework.core.invocation.MethodArgumentResolver`

Responsabilite :

- resoudre tous les arguments de la methode cible

Code a deplacer :

- la boucle sur `paramInfos`
- la resolution des objets, maps, session et fichiers

#### `mg.miniframework.core.scanning.ControllerScanner`

Responsabilite :

- scanner le classpath et trouver les controllers

Code a deplacer depuis `FrontControllerServlet` :

- `trouverClassesAvecAnnotation(...)`
- `scanPackagesForAnnotation(...)`
- `scanServletContextRecursively(...)`
- `loadClassSafely(...)`
- `hasAnnotationByName(...)`

#### `mg.miniframework.web.response.ExceptionResolver`

Responsabilite :

- traduire les exceptions en reponse HTTP coherente

#### `mg.miniframework.web.response.NotFoundResponseRenderer`

Responsabilite :

- centraliser le rendu de la reponse 404

---

## Tableau Mis A Jour Ancien Vers Cible

| Element | Etat actuel | Cible recommandee |
|---|---|---|
| `MethodManager` | deja dans `core.invocation`, encore monolithique | `ControllerMethodInvoker` fin + composants extraits |
| `FrontControllerServlet` | deja dans `web.servlet`, encore trop charge | facade HTTP fine |
| `CachedService` | deja dans `service.registry` | `ServiceRegistry` + implementation concrete |
| `FrameworkService` | deja dans `service.api` | clarifier si abstraite ou interface |
| `RouteMap` | deja dans `core.routing` | conserver |
| `CachedMethodInfo` | deja dans `core.routing` | conserver |
| `Url` | deja dans `core.routing` | conserver |
| `ConfigLoader` | deja dans `core.config` | conserver |
| `ContentRenderManager` | deja dans `web.response` | conserver |
| `MetricsManager` | deja dans `metrics` | conserver |
| `LogManager` | dans `loggin` | renommer vers `logging` |
| `DataTypeUtils` | encore centralisee | decomposition progressive |

---

## Sequence D'Execution La Plus Realiste Maintenant

### Etape 1

Stabiliser les deplacements deja faits.

1. renommer `loggin` vers `logging`
2. corriger les imports
3. nettoyer les warnings et imports inutiles
4. compiler

### Etape 2

Extraire `ServiceInjector`.

1. creer `mg.miniframework.service.injection.ServiceInjector`
2. y deplacer toute la logique d'injection `@Service`
3. appeler cette classe depuis `MethodManager`
4. compiler

### Etape 3

Extraire `RequestObjectBinder`.

1. creer `mg.miniframework.core.binding.RequestObjectBinder`
2. y deplacer `getObjectInstanceFromRequest(...)`
3. appeler cette classe depuis `MethodManager`
4. compiler

### Etape 4

Extraire la gestion multipart.

1. creer `mg.miniframework.web.multipart.MultipartRequestReader`
2. creer `MultipartRequestData`
3. deplacer la lecture des `Part`
4. remplacer la logique inline dans `MethodManager`
5. compiler

### Etape 5

Extraire `MethodArgumentResolver`.

1. deplacer la boucle de resolution des `paramInfos`
2. reduire `MethodManager` a un role d'orchestrateur
3. renommer ensuite la classe si necessaire

### Etape 6

Nettoyer `DataTypeUtils`.

1. creer les nouvelles classes utilitaires
2. migrer les appels progressivement
3. garder une facade temporaire
4. compiler a chaque sous-etape

### Etape 7

Simplifier `FrontControllerServlet`.

1. extraire `ControllerScanner`
2. extraire `RequestDispatcherEngine`
3. extraire `ExceptionResolver`
4. extraire `NotFoundResponseRenderer`
5. reduire `FrontControllerServlet`

---

## Ce Qu'Il Ne Faut Pas Faire Tout De Suite

Pour eviter de ralentir le chantier, ne faites pas maintenant :

1. une migration multi-module Maven complete
2. un renommage massif de tous les packages d'un coup
3. une suppression immediate de `DataTypeUtils`
4. une refonte totale de `CachedService` avant d'avoir termine le decoupage de `MethodManager`

---

## Resultat Attendu Apres Les Prochaines Etapes

Quand les prochaines etapes prioritaires seront terminees :

1. les packages deplaces seront stabilises
2. `MethodManager` ne sera plus le point de concentration principal
3. l'injection de services sera isolee
4. le binding de requete sera testable separement
5. `FrontControllerServlet` pourra enfin etre simplifie proprement
6. le projet sera pret pour une future separation multi-module si vous le souhaitez

---

## Prochaine Action Recommandee

La meilleure suite immediate est :

1. corriger `loggin` vers `logging`
2. extraire `ServiceInjector`
3. extraire `RequestObjectBinder`

C'est la suite la plus rentable compte tenu de ce que vous avez deja fait.
