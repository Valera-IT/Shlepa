# Fix Translation Issues and Improve Localization

Investigate why the "Sensitivity" menu item and its content are not translating correctly and ensure all menu items translate reliably across all supported languages.

## Proposed Changes

### UI & Localization Logic

#### [MainActivity.kt](file:///Users/valera/AndroidStudioProjects/Bang/app/src/main/java/com/example/shlepa/MainActivity.kt)

- **Wrap `SettingsMenu` in `key(viewModel.currentLanguageState.value)`**: This ensures the entire menu composable is recomposed when the language changes, forcing all `stringResource` calls inside it to use the new localized context from `LocalizationWrapper`.
- **Verify `SettingsMenu` internal `stringResource` calls**: Ensure all labels (`sensitivityLabel`, etc.) are read inside the composable so they react to the context provided by `LocalizationWrapper`.

## Verification Plan

### Manual Verification
1. **Change Language**: Open the language menu and switch to English, Russian, German, etc.
2. **Check Menu Labels**: Verify that "Theme", "Language", "Sensitivity", and "About app" translate correctly in the main settings menu.
3. **Check Sub-menus**:
    - Open "Theme" and verify theme names are translated.
    - Open "Sensitivity" and verify the title and the "Sensitivity threshold: X.X" text are translated.
4. **Check Main Screen**: Verify "SLAPS", "RECORD", and "Last impact force" are translated on the main screen.
5. **Restart App**: Ensure the selected language persists and everything remains translated.
