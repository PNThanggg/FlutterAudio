library meta_meta;

@Target({TargetKind.classType})
class Target {
  final Set<TargetKind> kinds;

  const Target(this.kinds);
}

enum TargetKind {
  classType,
  enumType,
  extension,
  field,
  function,
  library,
  getter,
  method,
  mixinType,
  parameter,
  setter,
  topLevelVariable,
  type,
  typedefType,
}

extension TargetKindExtension on TargetKind {
  String get displayString {
    switch (this) {
      case TargetKind.classType:
        return 'classes';
      case TargetKind.enumType:
        return 'enums';
      case TargetKind.extension:
        return 'extensions';
      case TargetKind.field:
        return 'fields';
      case TargetKind.function:
        return 'top-level functions';
      case TargetKind.library:
        return 'libraries';
      case TargetKind.getter:
        return 'getters';
      case TargetKind.method:
        return 'methods';
      case TargetKind.mixinType:
        return 'mixins';
      case TargetKind.parameter:
        return 'parameters';
      case TargetKind.setter:
        return 'setters';
      case TargetKind.topLevelVariable:
        return 'top-level variables';
      case TargetKind.type:
        return 'types (classes, enums, mixins, or typedefs)';
      case TargetKind.typedefType:
        return 'typedefs';
    }
  }
}