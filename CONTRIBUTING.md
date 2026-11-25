# Contributing to flutter_v2ray_client

Thank you for your interest in contributing to flutter_v2ray_client! 🎉 We welcome contributions from the community and are grateful for your help in making this project better.

## 📋 Table of Contents
- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [How to Contribute](#how-to-contribute)
- [Development Guidelines](#development-guidelines)
- [Testing](#testing)
- [Submitting Changes](#submitting-changes)
- [Reporting Issues](#reporting-issues)
- [Community](#community)

## 🤝 Code of Conduct

This project follows a code of conduct to ensure a welcoming environment for all contributors. By participating, you agree to:
- Be respectful and inclusive
- Focus on constructive feedback
- Accept responsibility for mistakes
- Show empathy towards other contributors
- Help create a positive community

## 🚀 Getting Started

### Prerequisites
- Flutter SDK (latest stable version)
- Android Studio or VS Code with Flutter extensions
- Git
- Android device/emulator for testing

### Quick Setup
1. Fork the repository
2. Clone your fork: `git clone https://github.com/your-username/flutter_v2ray_client.git`
3. Navigate to the project: `cd flutter_v2ray_client`
4. Install dependencies: `flutter pub get`
5. Run the example app: `flutter run`

## 🛠️ Development Setup

### Local Development
```bash
# Clone the repository
git clone https://github.com/amir-zr/flutter_v2ray_client.git
cd flutter_v2ray_client

# Install dependencies
flutter pub get

# Run tests
flutter test

# Run the example app
cd example
flutter run
```

### Android Native Development
For Android-specific changes:
1. Open Android module in Android Studio: `packages/flutter_v2ray_client/android`
2. Build the AAR: `./gradlew assembleDebug`
3. Test with the example app

### Rebuild the embedded Android Xray core

Use JDK 21 and set `ANDROID_SDK_ROOT` to an Android SDK with NDK
`29.0.14206865`. The build pins the AndroidLibXrayLite sources, Go, and
gomobile, then applies the Android socket protection and traffic counter
bindings from `tool/android_xray/`.

```sh
./tool/build_android_xray.sh /path/to/libv2ray.aar
```

Validate the generated Java bindings, Android API 21 manifest, and 16 KiB
ELF load alignment for each ABI before replacing `android/libs/libv2ray.aar`.
Exercise connect, traffic, disconnect, and reconnect on physical Android
through the consuming app before updating its Git dependency.

## 💡 How to Contribute

### Types of Contributions
- 🐛 **Bug Fixes**: Fix existing issues
- ✨ **Features**: Add new functionality
- 📚 **Documentation**: Improve docs or add examples
- 🧪 **Tests**: Add or improve test coverage
- 🎨 **UI/UX**: Improve user interface
- 🚀 **Performance**: Optimize performance
- 🌐 **Platform Support**: Add support for new platforms

### Finding Issues to Work On
1. Check [GitHub Issues](https://github.com/amir-zr/flutter_v2ray_client/issues)
2. Look for issues labeled `good first issue` or `help wanted`
3. Check the [Roadmap](./README.md#roadmap--future-enhancements) for planned features

## 📝 Development Guidelines

### Code Style
- Follow Dart's [official style guide](https://dart.dev/guides/language/effective-dart/style)
- Use `flutter format` to format your code
- Run `flutter analyze` to check for issues

### Commit Messages
Use clear, descriptive commit messages:
```
feat: add new V2Ray connection method
fix: resolve memory leak in Android VPN service
docs: update API documentation
test: add unit tests for URL parser
```

### Naming Conventions
- Classes: `PascalCase` (e.g., `V2rayService`)
- Methods: `camelCase` (e.g., `startV2Ray()`)
- Variables: `camelCase` (e.g., `connectionStatus`)
- Constants: `SCREAMING_SNAKE_CASE` (e.g., `DEFAULT_TIMEOUT`)

### Architecture Guidelines
- Follow Flutter's widget lifecycle best practices
- Use Provider/Riverpod for state management
- Implement proper error handling
- Write platform-specific code in respective directories
- Keep business logic separate from UI code

## 🧪 Testing

### Running Tests
```bash
# Run all tests
flutter test

# Run tests with coverage
flutter test --coverage

# Run specific test file
flutter test test/v2ray_service_test.dart
```

### Writing Tests
- Write unit tests for business logic
- Write widget tests for UI components
- Write integration tests for end-to-end functionality
- Aim for high test coverage (>80%)
- Use descriptive test names

### Test Structure
```
test/
├── unit/
│   ├── services/
│   └── utils/
├── widget/
│   └── components/
└── integration/
    └── flows/
```

## 📤 Submitting Changes

### Pull Request Process
1. **Fork** the repository
2. **Create** a feature branch: `git checkout -b feature/amazing-feature`
3. **Make** your changes following the guidelines
4. **Test** thoroughly
5. **Commit** with clear messages
6. **Push** to your fork: `git push origin feature/amazing-feature`
7. **Create** a Pull Request

### Pull Request Template
Please fill out the PR template with:
- Description of changes
- Type of change (bug fix, feature, docs, etc.)
- Testing instructions
- Screenshots (if UI changes)
- Related issues

### Review Process
- All PRs require review before merging
- Maintainers may request changes
- CI/CD must pass all checks
- At least one maintainer approval required

## 🐛 Reporting Issues

### Bug Reports
When reporting bugs, please include:
- **Title**: Clear, descriptive title
- **Description**: Detailed description of the issue
- **Steps to Reproduce**: Step-by-step instructions
- **Expected Behavior**: What should happen
- **Actual Behavior**: What actually happens
- **Environment**: Flutter version, OS, device
- **Logs**: Error messages, stack traces
- **Screenshots**: Visual evidence of the issue

### Feature Requests
For new features, please include:
- **Title**: Clear feature title
- **Description**: Detailed description of the feature
- **Use Case**: Why this feature is needed
- **Proposed Solution**: How you think it should work
- **Alternatives**: Other solutions you've considered

## 🌟 Community

### Communication Channels
- **GitHub Issues**: For bugs and feature requests
- **GitHub Discussions**: For general questions and discussions
- **Pull Requests**: For code contributions

### Getting Help
- Check existing issues and documentation first
- Use clear, descriptive titles for issues
- Provide as much context as possible
- Be patient and respectful

### Recognition
Contributors will be:
- Listed in the project's contributors
- Mentioned in release notes
- Acknowledged in the README

## 📄 License

By contributing to flutter_v2ray_client, you agree that your contributions will be licensed under the same license as the project (MIT License).

---

Thank you for contributing to flutter_v2ray_client! 🚀 Your efforts help make this project better for everyone in the community.
