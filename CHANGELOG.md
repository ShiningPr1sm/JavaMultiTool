# Changelog

All notable changes to JMT project will be documented in this file.

## [Unreleased]
### Added
**Photo & Video Panel:**
- File Organizer

**Math Panel:**
- Unit Converter

**Text Panel:**
- Find & Replace

## 🧩 [0.1.4] - 2026-07-15
### HOTFIX v0.1.4

### Added
**Core:**
- Added a default profile icon for new users ([#4](https://github.com/ShiningPr1sm/JavaMultiTool/issues/4)). by @EmaniPreethika

### Fixed
**Core:**
- Fixed a bug where all users shared the same data. User data is now stored in separate `*.db` files (except `user_data.db`).
  If you're upgrading from an older version, your existing data will automatically transfer to the first account you log into.

**Full Changelog**: https://github.com/ShiningPr1sm/JavaMultiTool/compare/v0.1.3...v0.1.4

## 🧩 [0.1.3] - 2026-07-15
### Added
**Core:**
- Additional device information has been added to the settings panel
- Now, you earn 20 XP each time you open the app (once per day). A small bonus :)
- Three new app themes have been added: "Calm Tech," "Night Energy," and "Blush Pink"
- The program no longer creates a new instance or refreshes the page every time you click on it if you're already on that page

**Photo & Video panel:**
- The "Image Tools" tab can now delete, save, or copy metadata extracted from photos
- Added a palette of the 10 dominant colors in a photo — tap to copy their HEX code
- Added ELA (Error Level Analysis) — highlights likely edited/inserted areas of a photo based on JPEG compression artifacts

**Time panel:**
- The "Workflow" tab has been redesigned. It now consists of 3 sections:
  - "Worklog" — manually create tasks and track time spent on them
  - Pomodoro timer — set a timer and focus on work, study, or other activities
  - "AppTracker" — automatically records time spent running specific programs of your choice. The tab now includes an editable list (delete or rename tracked apps)
  - "Overview" — remains almost the same
- Each tracked app now has its own assigned color, generated automatically via a hash function
- Renamed "BDay notifier" to "Birthday Tracker"
- "Birthday Tracker" tab redesigned to match Workflow's style — tab system, custom JTextFields, table view, etc.
- Birthdays are now automatically synced between the Edit tab and Overview tab

**New: Utils panel:**
- Added 4 new tools:
  - Color Picker & Converter — pick a color from the screen or enter one manually in RGB, HEX, or HSL
  - Password Generator — generate passwords in various formats and check their strength
  - QR Generator & Decoder — generate a QR code or decode an existing one
  - Network Tools — check your current IP, test if a website's port is open, ping a host, or look up WHOIS info

### Fixed
**Core:**
- All pop-up windows now match the app's design ([#2](https://github.com/ShiningPr1sm/JavaMultiTool/issues/2))

**Photo & Video panel:**
- Fixed video uploads not working correctly in the Media Downloader tab

**Time panel:**
- Fixed a bug where switching tabs would make a manually created task invisible

**Full Changelog**: https://github.com/ShiningPr1sm/JavaMultiTool/compare/v0.1.2...v0.1.3

## 🧩 [0.1.2] - 2026-07-11
### Added
- A bar displaying the current and latest version has been added to the bottom of the program, along with a button to open the GitHub project
- Added the `AppLogger` class across all program classes for more convenient debugging
- Added a quote display on the main page. You can now return to the main page by left-clicking the avatar in the top-left corner

### Fixed
- Fixed a bug where the notification counter above the bell icon would disappear
- Buttons in the settings panel are now consistently sized relative to other components
- The "Local IP" label in the settings panel now works correctly
- Navigating to the settings panel no longer triggers an animation
- Removed a slight lag when navigating to the achievements/settings panel
- Fixed `updateLastLoginDate` not being called during auto-login, which prevented the last login date from updating in settings
- Changed the date display format from "yyyy-MM-dd HH:mm:ss" to "HH:mm:ss / dd.MM.yyyy"
- Removed the old "original_dark" theme name, which could cause errors
- Centered the flying dots animation; connecting lines no longer overlap the dots and are now positioned below them

**Full Changelog**: https://github.com/ShiningPr1sm/JavaMultiTool/compare/v0.1.1...v0.1.2

## 🧩 [0.1.1] - 2026-07-08
### Added
- Added a notifications tab. For now, only birthdays are included — reminders are sent 7, 3, and 1 day in advance. This will become customizable in the future

### Changed
- Updated the program's title

**Full Changelog**: https://github.com/ShiningPr1sm/JavaMultiTool/compare/v0.1.0...v0.1.1

## 🧩 [0.1.0] - 2026-07-08
### 🎊 First release of JMT / JavaMultiTool!

JMT brings together the useful features scattered across various corners of the web — websites and apps — into one place. No ads, spam, or subscriptions.

### Added
- Video downloader
- App time tracker
- Birthday calendar
- Basic photo management tools

**Full Changelog**: https://github.com/ShiningPr1sm/JavaMultiTool/commits/v0.1.0
