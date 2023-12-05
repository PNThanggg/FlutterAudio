// ignore_for_file: library_private_types_in_public_api

library meta;

import 'meta_meta.dart';

const _DoNotStore doNotStore = _DoNotStore();

const _Experimental experimental = _Experimental();

const _Factory factory = _Factory();

const Immutable immutable = Immutable();

const _Internal internal = _Internal();


const _IsTest isTest = _IsTest();

const _IsTestGroup isTestGroup = _IsTestGroup();

const _Literal literal = _Literal();

const _MustBeOverridden mustBeOverridden = _MustBeOverridden();

const _MustCallSuper mustCallSuper = _MustCallSuper();

const _NonVirtual nonVirtual = _NonVirtual();

const _OptionalTypeArgs optionalTypeArgs = _OptionalTypeArgs();

const _Protected protected = _Protected();

const _Reopen reopen = _Reopen();

const Required required = Required();

const _Sealed sealed = _Sealed();

const UseResult useResult = UseResult();

const _VisibleForOverriding visibleForOverriding = _VisibleForOverriding();

const _VisibleForTesting visibleForTesting = _VisibleForTesting();


class Immutable {
  final String reason;
  const Immutable([this.reason = '']);
}

class Required {
  final String reason;

  const Required([this.reason = '']);
}

/// See [useResult] for more details.
@Target({
  TargetKind.field,
  TargetKind.function,
  TargetKind.getter,
  TargetKind.method,
  TargetKind.topLevelVariable,
})
class UseResult {
  final String reason;

  final String? parameterDefined;

  const UseResult([this.reason = '']) : parameterDefined = null;

  const UseResult.unless({required this.parameterDefined, this.reason = ''});
}

@Target({
  TargetKind.classType,
  TargetKind.function,
  TargetKind.getter,
  TargetKind.library,
  TargetKind.method,
})
class _DoNotStore {
  const _DoNotStore();
}

class _Experimental {
  const _Experimental();
}

class _Factory {
  const _Factory();
}

class _Internal {
  const _Internal();
}

class _IsTest {
  const _IsTest();
}

class _IsTestGroup {
  const _IsTestGroup();
}

class _Literal {
  const _Literal();
}

@Target({
  TargetKind.field,
  TargetKind.getter,
  TargetKind.method,
  TargetKind.setter,
})
class _MustBeOverridden {
  const _MustBeOverridden();
}

@Target({
  TargetKind.field,
  TargetKind.getter,
  TargetKind.method,
  TargetKind.setter,
})
class _MustCallSuper {
  const _MustCallSuper();
}

class _NonVirtual {
  const _NonVirtual();
}

@Target({
  TargetKind.classType,
  TargetKind.extension,
  TargetKind.function,
  TargetKind.method,
  TargetKind.mixinType,
  TargetKind.typedefType,
})
class _OptionalTypeArgs {
  const _OptionalTypeArgs();
}

class _Protected {
  const _Protected();
}

@Target({
  TargetKind.classType,
  TargetKind.mixinType,
})
class _Reopen {
  const _Reopen();
}

class _Sealed {
  const _Sealed();
}

class _VisibleForOverriding {
  const _VisibleForOverriding();
}

class _VisibleForTesting {
  const _VisibleForTesting();
}