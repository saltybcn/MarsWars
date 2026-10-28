# Mars Wars

**A Java strategy and tower-defense game set in a Martian colony.**

Mars Wars is an academic object-oriented programming project built with Java and Swing. Players protect a colony's oxygen core against waves of ground and flying enemies by deploying defenses, managing limited capacity, upgrading units, and progressing through missions.

The project emphasizes concurrent gameplay, configurable units, persistence, and a desktop interface.

## Key Features

- **Grid-based combat:** a 25 × 25 battlefield with an oxygen core at its center.
- **Strategic deployment:** place and remove defensive units, manage capacity, and improve health and damage.
- **Campaign progression:** advance through missions, replay encounters, and inspect battle results.
- **Concurrent unit behavior:** individual combatant threads coordinate movement, attacks, pausing, and termination.
- **Save system:** create, load, overwrite, and delete saved games.
- **Unit administration:** configure units and manage custom sprites and sound effects.
- **Combat feedback:** live statistics, damage history, animations, and audio cues.

## Technology Stack

| Area | Technology |
| --- | --- |
| Language | Java |
| Desktop UI | Swing, NetBeans GUI Builder |
| Build system | Maven |
| JSON handling | Gson 2.11.0 |
| Persistence | JSON catalogs and serialized save files |
| Concurrency | Java threads and synchronization |
| Design | Object-oriented programming, inheritance, polymorphism |

## Software Design

Mars Wars uses object-oriented components to separate gameplay, unit behavior, administration, and persistence.

- **Combat and concurrency:** combatants operate independently while shared game state is coordinated through synchronization.
- **Game state:** the map tracks positions, placement rules, and the oxygen core.
- **Unit configuration:** an editable catalog defines available units and their properties.
- **Persistence:** saved games preserve campaign state; loading does not automatically start combat threads.
- **Presentation:** Swing forms provide gameplay controls, unit management, and visual feedback.

The project's class diagram is available in [SVG](UML_clases.svg) and [PDF](UML_clases.pdf) formats.

## Getting Started

### Prerequisites

- JDK 26
- Apache Maven
- Apache NetBeans (recommended for editing Swing forms)

### Run from NetBeans

1. Clone the repository:

   ```bash
   git clone https://github.com/saltybcn/MarsWars.git
   ```

2. Open `Proyecto_1/pom.xml` in NetBeans.
3. Configure JDK 26 and run **Clean and Build**.
4. Run the `Main.Proyecto_1` main class.

The main menu supports starting or loading a game and opening the unit administrator.

### Build from the command line

From `Proyecto_1/`, run:

```bash
mvn clean package dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/lib
```

To run the packaged application, keep the generated JAR, `target/lib` dependencies, `recursos/` directory, and external catalog in the expected relative locations.

Example distribution layout:

```text
MarsWars/
├── Proyecto_1-1.0-SNAPSHOT.jar
├── lib/
├── recursos/
└── src/main/java/persistencia/catalogo.json
```

From the distribution directory:

**Windows**
```bat
java -cp "Proyecto_1-1.0-SNAPSHOT.jar;lib/*" Main.Proyecto_1
```

**Linux / macOS**
```bash
java -cp "Proyecto_1-1.0-SNAPSHOT.jar:lib/*" Main.Proyecto_1
```

The runtime requires Java 26. This repository documents a manual JAR distribution; it does not provide a prebuilt native installer. Do not distribute personal save files or administrator credentials.

## Gameplay Details

The oxygen core occupies the center of the 25 × 25 grid. Enemies spawn outside a protected region surrounding the core. Players place defenses using the left mouse button and remove them using the right mouse button.

Each mission provides a battle summary and combat history. Continuing after a mission increases available capacity by five, while unit upgrades improve health or damage. Replaying a mission restores its original enemy arrangement and the core's health.

Save files are stored under `partidas/`. The administrator supports creating, editing, disabling, and deleting unit configurations. Custom media files are managed separately from bundled application resources.

## Project Structure

| Path | Purpose |
| --- | --- |
| `Proyecto_1/` | Maven application and source code |
| `Proyecto_1/src/main/java/` | Java implementation |
| `Proyecto_1/src/main/resources/` | Bundled assets and sounds |
| `Proyecto_1/recursos/` | External unit images and audio |
| `Proyecto_1/src/main/java/persistencia/catalogo.json` | Editable unit catalog |
| `Proyecto_1/src/test/java/` | Project test classes |
| `UML_clases.svg` / `UML_clases.pdf` | UML class diagram |

## Testing

The project includes tests covering model behavior, persistence, resources, audio, and Swing components. Functional, visual, and audio behavior were also checked through NetBeans during development.

Some tests write files and should be run from a clean temporary workspace to avoid modifying personal data.

## Project Context

Mars Wars was developed as a university object-oriented programming project. Its focus is on applying Java software-engineering concepts to an interactive application, including class hierarchies, concurrent execution, state management, serialization, and GUI development.

**Status:** Academic project. Source code and technical documentation are provided for review and learning.

## License

Original source code and original project documentation are licensed under the
[MIT License](LICENSE). Third-party dependencies and media retain their own terms;
see [third-party notices](THIRD_PARTY_NOTICES.md).
