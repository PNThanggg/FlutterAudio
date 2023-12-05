import 'meta/meta.dart';

abstract class PlatformInterface {
  PlatformInterface({required Object token}) {
    _instanceTokens[this] = token;
  }

  static final Expando<Object> _instanceTokens = Expando<Object>();

  static void verify(PlatformInterface instance, Object token) {
    _verify(instance, token, preventConstObject: true);
  }

  static void verifyToken(PlatformInterface instance, Object token) {
    _verify(instance, token, preventConstObject: false);
  }

  static void _verify(
    PlatformInterface instance,
    Object token, {
    required bool preventConstObject,
  }) {
    if (instance is MockPlatformInterfaceMixin) {
      bool assertionsEnabled = false;
      assert(() {
        assertionsEnabled = true;
        return true;
      }());
      if (!assertionsEnabled) {
        throw AssertionError(
            '`MockPlatformInterfaceMixin` is not intended for use in release builds.');
      }
      return;
    }

    if (preventConstObject &&
        identical(_instanceTokens[instance], const Object())) {
      throw AssertionError('`const Object()` cannot be used as the token.');
    }

    if (!identical(token, _instanceTokens[instance])) {
      throw AssertionError(
          'Platform interfaces must not be implemented with `implements`');
    }
  }
}

@visibleForTesting
abstract class MockPlatformInterfaceMixin implements PlatformInterface {}
