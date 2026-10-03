# Developer API

Homestead provides a public API for developers to integrate with the plugin. You can manage regions, chunks, members, flags, and more programmatically, and listen to events fired for every major action.

---

## Installation

### Authentication

GitHub Packages requires authentication for **every** download, including packages in a public repository. An unauthenticated request is rejected with `401 Unauthorized`, which Gradle surfaces as `Could not resolve ... Received status code 401 from server`.

!!! warning "Authentication is required even for public packages"

    This is the single most common reason the API fails to resolve. It is a GitHub restriction, not a problem with your build file.

Create a [personal access token](https://github.com/settings/tokens/new) with the **`read:packages`** scope enabled.

!!! danger "Missing `read:packages` scope"

    A token without the `read:packages` scope is treated as if the package does not exist: GitHub returns `404 Not Found`. If you see a 404, check the token's scopes before anything else.

=== "Maven"

    Add a `<server>` to `~/.m2/settings.xml`. The `<id>` **must** match the `<id>` of the `<repository>` declared below, otherwise Maven ignores the credentials and still sends an anonymous request.

    ```xml
    <settings>
      <servers>
        <server>
          <id>homestead-github</id>
          <username>YOUR_GITHUB_USERNAME</username>
          <password>YOUR_PERSONAL_ACCESS_TOKEN</password>
        </server>
      </servers>
    </settings>
    ```

=== "Gradle"

    Put the credentials in `~/.gradle/gradle.properties` so they never end up in version control:

    ```properties
    GitHubPackagesUsername=YOUR_GITHUB_USERNAME
    GitHubPackagesPassword=YOUR_PERSONAL_ACCESS_TOKEN
    ```

    Then bind them to the repository declared below:

    ```kotlin
    credentials {
        username = providers.gradleProperty("GitHubPackagesUsername").orNull
        password = providers.gradleProperty("GitHubPackagesPassword").orNull
    }
    ```

### Maven

```xml
<repository>
    <id>homestead-github</id>
    <url>https://maven.pkg.github.com/TayebYassine/Homestead</url>
</repository>

<dependency>
    <groupId>me.tayebyassine.homestead</groupId>
    <artifactId>homestead</artifactId>
    <version>6.0.0.0</version>
    <classifier>api</classifier>
    <scope>provided</scope>
</dependency>
```

### Gradle

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/TayebYassine/Homestead")
        credentials {
            username = providers.gradleProperty("GitHubPackagesUsername").orNull
            password = providers.gradleProperty("GitHubPackagesPassword").orNull
        }
    }
}

dependencies {
    compileOnly("me.tayebyassine.homestead:homestead:6.0.0.0:api")
}
```

!!! note "Always use the `api` classifier"

    The classifier-less artifact (`homestead-6.0.0.0.jar`) is the shaded server jar bundled with Homestead's dependencies. The `api` classifier (`homestead-6.0.0.0-api.jar`) is the artifact intended for compilation.

!!! info "Browsing the registry URL returns 404"

    Opening `https://maven.pkg.github.com/TayebYassine/Homestead` in a browser always returns `404 Not Found`. That endpoint has no web interface. The package and its version list live at [github.com/TayebYassine/Homestead/packages/3276486](https://github.com/TayebYassine/Homestead/packages/3276486).

### Troubleshooting

| Symptom                                              | Cause                                                                           | Fix                                                                                                 |
|:-----------------------------------------------------|:--------------------------------------------------------------------------------|:----------------------------------------------------------------------------------------------------|
| `Received status code 401 from server: Unauthorized` | No credentials sent to GitHub Packages                                          | Add the `credentials {}` block (Gradle) or the matching `<server>` entry (Maven)                    |
| `Received status code 404 from server: Not Found`    | Token exists but lacks `read:packages`, or the coordinates/classifier are wrong | Regenerate the token with `read:packages`; verify `me.tayebyassine.homestead:homestead:6.0.0.0:api` |
| Maven ignores your credentials                       | `<server>` `<id>` does not match the `<repository>` `<id>`                      | Make both `homestead-github`                                                                        |
| Resolves, but classes are missing from the jar       | Used the classifier-less artifact                                               | Add the `:api` classifier                                                                           |

---

## JavaDoc

Full API documentation is available:

- [JavaDoc Home](https://tayebyassine.github.io/Homestead/javadoc/)
- [Events Package](https://tayebyassine.github.io/Homestead/javadoc/me/tayebyassine/homestead/api/events/package-summary.html)
- [Managers Package](https://tayebyassine.github.io/Homestead/javadoc/me/tayebyassine/homestead/managers/package-summary.html)
- [Models Package](https://tayebyassine.github.io/Homestead/javadoc/me/tayebyassine/homestead/models/package-summary.html)

!!! warning "Import the right package"

    The API lives under `me.tayebyassine.homestead.*`. Older guides referencing `tfagaming.projects.minecraft.homestead.*` are outdated, those classes no longer exist.

---

## API Events

Homestead fires events for many actions. Listen to them like any Bukkit event by implementing `Listener` and annotating handler methods with `@EventHandler`.

Events are split across sub-packages of `me.tayebyassine.homestead.api.events`, so a single wildcard import will **not** pull them in. Import each package you use:

```java
import me.tayebyassine.homestead.api.events.chunk.ChunkClaimEvent;
import me.tayebyassine.homestead.api.events.region.RegionCreateEvent;
import me.tayebyassine.homestead.api.events.player.PlayerJoinRegionEvent;
import me.tayebyassine.homestead.api.events.economy.BankDepositEvent;
import me.tayebyassine.homestead.api.events.communication.RegionChatEvent;
```

| Sub-package | Events |
|:------------|:-------|
| `api.events.chunk` | `ChunkClaimEvent`, `ChunkUnclaimEvent` |
| `api.events.region` | `RegionCreateEvent`, `RegionDeleteEvent`, `RegionNameUpdateEvent`, `RegionDisplaynameUpdateEvent`, `RegionDescriptionUpdateEvent`, `RegionLocationUpdateEvent`, `RegionOwnerUpdateEvent` |
| `api.events.player` | `PlayerJoinRegionEvent`, `PlayerLeftRegionEvent`, `PlayerJoinSubAreaEvent`, `PlayerLeftSubAreaEvent`, `InvitePlayerEvent`, `RevokePlayerInviteEvent`, `BulkDeleteInvitesEvent`, `BanPlayerEvent`, `UnbanPlayerEvent`, `BulkUnbanPlayersEvent` |
| `api.events.economy` | `BankDepositEvent`, `BankWithdrawEvent` |
| `api.events.communication` | `RegionChatEvent`, `PlayerMailEvent` |
| `api.events.menu` | `MenuOpenEvent`, `MenuCloseEvent`, `MenuClickEvent`, `MenuButtonRenderEvent`, `MenuPageChangeEvent` |
| `api.events.rent` | `RentStartEvent`, `RentEndEvent`, `RentExpireEvent`, `RentNoticeEvent`, `RentConfigUpdateEvent` |
| `api.events.war` | `WarKillEvent`, `WarOwnershipTransferEvent` |

Several lifecycle events are **cancellable**. Call `event.setCancelled(true)` to abort the action before it completes:

- `RegionCreateEvent`
- `RegionDeleteEvent`
- `ChunkClaimEvent`
- `ChunkUnclaimEvent`
- `BanPlayerEvent`
- `UnbanPlayerEvent`
- `MenuButtonRenderEvent`
- `MenuClickEvent`
- `MenuPageChangeEvent`
- `RentStartEvent`
- `RentEndEvent`

### Region Lifecycle

| Event | Fired When |
|:------|:-----------|
| `RegionCreateEvent` | A region is created |
| `RegionDeleteEvent` | A region is deleted |

### Chunk Actions

| Event | Fired When |
|:------|:-----------|
| `ChunkClaimEvent` | A chunk is claimed |
| `ChunkUnclaimEvent` | A chunk is unclaimed |

### Player Movement

| Event | Fired When |
|:------|:-----------|
| `PlayerJoinRegionEvent` | A player enters a region |
| `PlayerLeftRegionEvent` | A player leaves a region |
| `PlayerJoinSubAreaEvent` | A player enters a sub-area |
| `PlayerLeftSubAreaEvent` | A player leaves a sub-area |

### Member Management

| Event | Fired When |
|:------|:-----------|
| `InvitePlayerEvent` | A player is invited to a region |
| `RevokePlayerInviteEvent` | A player's invite is revoked |
| `BulkDeleteInvitesEvent` | Multiple invites are deleted |

### Ban Management

| Event | Fired When |
|:------|:-----------|
| `BanPlayerEvent` | A player is banned from a region |
| `UnbanPlayerEvent` | A player is unbanned |
| `BulkUnbanPlayersEvent` | Multiple players are unbanned |

### Banking

| Event | Fired When |
|:------|:-----------|
| `BankDepositEvent` | Money is deposited to a region bank |
| `BankWithdrawEvent` | Money is withdrawn from a region bank |

### Communication

| Event | Fired When |
|:------|:-----------|
| `RegionChatEvent` | A message is sent in region chat |
| `PlayerMailEvent` | A player receives mail |

### Region Updates

| Event | Fired When |
|:------|:-----------|
| `RegionNameUpdateEvent` | Region name is changed |
| `RegionDisplaynameUpdateEvent` | Region display name is changed |
| `RegionDescriptionUpdateEvent` | Region description is changed |
| `RegionLocationUpdateEvent` | Region location data is updated |
| `RegionOwnerUpdateEvent` | Region ownership is transferred |

### Menus

| Event | Fired When |
|:------|:-----------|
| `MenuOpenEvent` | A Homestead GUI is opened |
| `MenuCloseEvent` | A Homestead GUI is closed |
| `MenuClickEvent` | A button inside a Homestead GUI is clicked |
| `MenuButtonRenderEvent` | A button is being rendered, allowing its icon or label to be changed |
| `MenuPageChangeEvent` | The player navigates to a different page of a paginated menu |

### Rent

| Event                   | Fired When                                 |
|:------------------------|:-------------------------------------------|
| `RentStartEvent`        | A rent agreement starts                    |
| `RentEndEvent`          | A rent agreement ends normally             |
| `RentExpireEvent`       | A rent agreement expires                   |
| `RentNoticeEvent`       | A rent reminder is sent to the tenant      |
| `RentConfigUpdateEvent` | Rent configuration for a region is changed |

### Wars

| Event | Fired When |
|:------|:-----------|
| `WarKillEvent` | A kill is scored during a war |
| `WarOwnershipTransferEvent` | A region changes hands as the result of a war |

---

## API Usage Examples

See [API Examples](API%20Examples.md) for code examples covering regions, chunks, members, flags, and events.

