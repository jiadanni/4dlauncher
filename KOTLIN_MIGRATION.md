# Kotlin Migration Guide

This project is being migrated from Java to Kotlin. Kotlin support has been enabled and the migration is in progress.

## Current Status

✅ **Completed:**
- Kotlin plugin enabled in build.gradle
- Kotlin standard library added (1.9.22)
- AndroidX Core KTX added (1.12.0)
- **5 files converted to Kotlin:**
  - `PermissionChecker.kt` - Permission handling utility
  - `DialogHelper.kt` - Dialog creation utilities
  - `Tool.kt` - General utility functions (20+ methods)
  - `App.kt` - App model (Kotlin data class with backward compatibility)
  - `Item.kt` - Item model with factory methods and Type enum

📝 **Remaining:**
- 47+ Java files to convert
- Activity classes
- Fragment classes
- Widget classes
- Other utility and model classes

## Benefits of Kotlin

1. **Null Safety** - Eliminates NullPointerException crashes
2. **Concise Code** - ~40% less code compared to Java
3. **Modern Language Features** - Coroutines, extension functions, data classes
4. **100% Java Interop** - Kotlin and Java can coexist in the same project
5. **Official Android Language** - Google recommends Kotlin for Android development

## How to Continue Migration

### Option 1: Using Android Studio (Recommended)

1. Open the project in Android Studio
2. Select a Java file
3. Go to `Code` → `Convert Java File to Kotlin File`
4. Review the conversion and make adjustments
5. Test the converted code
6. Repeat for all files

**Tips:**
- Convert utility classes first (simpler, fewer dependencies)
- Then convert models and data classes
- Finally convert Activities, Fragments, and Views
- Test frequently after each conversion

### Option 2: Manual Conversion

Use the `PermissionChecker.kt` as a reference for conversion patterns:

**Java → Kotlin Patterns:**

```kotlin
// Constructor
// Java: public PermissionChecker(Activity activity) { _activity = activity; }
// Kotlin: class PermissionChecker(protected val activity: Activity)

// Varargs
// Java: String... args
// Kotlin: vararg args: String?

// Null checks
// Java: if (array != null && array.length > 0 && array[0] != null)
// Kotlin: if (array.isNotEmpty() && array[0] != null)

// When instead of switch
// Java: switch(x) { case A: ... case B: ... }
// Kotlin: when(x) { A -> ... B -> ... }

// String templates
// Java: "Value: " + value
// Kotlin: "Value: $value"

// Smart casts (no explicit casting needed)
// Destructuring: val (a, b) = Pair(1, 2)
// Extension functions
// Data classes
```

### Option 3: Gradual Migration (Current Approach)

Java and Kotlin can coexist. Migrate incrementally:

1. ✅ New features → Write in Kotlin
2. ✅ Bug fixes → Convert file to Kotlin
3. ✅ Refactoring → Convert related files to Kotlin

## Converted Files

- [x] `net/gsantner/opoc/util/PermissionChecker.kt`
- [ ] `com/benny/openlauncher/activity/HomeActivity.java`
- [ ] `com/benny/openlauncher/viewutil/DialogHelper.java`
- [ ] ... (50+ more files)

## Testing After Conversion

After converting files, ensure:

1. **Build succeeds**: `./gradlew assembleDebug`
2. **No warnings**: Check for Kotlin-specific warnings
3. **Runtime testing**: Test all features that use converted code
4. **Memory checks**: Kotlin's immutability helps, but verify no leaks

## Additional Kotlin Features to Adopt

Once migration is complete, consider:

1. **Coroutines** - Replace AsyncTask and callbacks
2. **Flow** - Reactive data streams
3. **Data Classes** - For model objects
4. **Sealed Classes** - For type-safe state management
5. **Extension Functions** - Add utility methods to existing classes
6. **ViewBinding** - Already enabled, can replace Butterknife

## Migration Priority

### High Priority (Core Utilities)
1. Permission handling ✅
2. App settings
3. Database helpers
4. Manager classes

### Medium Priority (UI Components)
5. Activities
6. Fragments
7. Custom Views

### Low Priority (Can Stay Java)
8. Third-party code (net.gsantner.opoc - except what's needed)
9. Generated code
10. Legacy components scheduled for removal

## Resources

- [Kotlin Official Docs](https://kotlinlang.org/docs/home.html)
- [Android Kotlin Guide](https://developer.android.com/kotlin)
- [Java to Kotlin Interop](https://kotlinlang.org/docs/java-interop.html)
- [Kotlin Style Guide](https://developer.android.com/kotlin/style-guide)

## Notes

- All new code should be written in Kotlin
- Java → Kotlin conversion is irreversible (one-way migration)
- Test thoroughly after each conversion
- Kotlin compilation is slightly slower than Java (acceptable trade-off)
