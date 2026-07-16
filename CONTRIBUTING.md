# Contributing to JavaMultiTool

First off, thanks for considering contributing! Whether it's a bug report, a new feature, or a typo fix — it's genuinely appreciated.

## Reporting bugs

Before opening an issue, please check the [Issues](https://github.com/ShiningPr1sm/JavaMultiTool/issues) page to see if it's already been reported.

When reporting a bug, please include:
- What you did (steps to reproduce)
- What you expected to happen
- What actually happened (screenshots help a lot)
- Your OS and Java version
- App version (shown at the bottom of the program)

## Suggesting features

Feature requests are welcome! Open an issue and describe:
- What problem it solves / why it'd be useful
- Roughly how you imagine it working

Not every suggestion will be implemented, but all of them are read and considered.

## Setting up the project locally

**Prerequisites**
- Java Development Kit (JDK) 21+
- Maven

**Steps**
```bash
git clone https://github.com/ShiningPr1sm/JavaMultiTool.git
cd JavaMultiTool
mvn clean install
```

Run the app from your IDE, or via:
```bash
mvn exec:java
```

## Submitting changes

1. Fork the repository
2. Create a branch from `main`: `git checkout -b fix/short-description` or `feature/short-description`
3. Make your changes
4. Test that the app builds and runs without errors
5. Commit with a clear message describing what changed and why
6. Open a Pull Request against `main`, describing what the PR does and linking any related issue (e.g. `closes #12`)

## Code style

- Follow the existing project structure (Dao/Repository separation, UI components under `ui/`)
- Keep UI styling consistent with `UIStyle` — avoid hardcoding colors/fonts outside of it
- Prefer clear, descriptive names over comments explaining unclear ones
- Use `AppLogger` for logging instead of `System.out.println`

## License

By contributing, you agree that your contributions will be licensed under the same [MIT License](LICENSE) that covers this project.

---

Thanks again for taking the time to contribute — every bit helps! 🎉
