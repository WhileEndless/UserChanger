# User Changer Burp Suite Extension

## Description
User Changer is a Burp Suite extension that allows users to modify HTTP request headers and body content. Through customizable profiles, modifications can be applied automatically. Users can define match-and-replace rules to modify headers and body content based on predefined patterns.

## Features
- Modify HTTP headers based on user-defined profiles.
- Switch between profiles for quick adjustments.
- Define match-and-replace rules for dynamic content modification.
- Create, edit, and delete user profiles.
- Apply regex-based changes to HTTP request bodies.

## Installation
1. Start Burp Suite.
2. Navigate to the "Extender" tab.
3. Click "Add" and load the extension.
4. After loading, the "User Changer" tab will appear in the Burp Suite interface.

## Usage
1. **Creating a Profile:**
   - Navigate to the "User Changer" tab.
   - Enter a profile name in the "Profile Name" field.
   - Add the headers in raw format in the "Headers" section.
   - Define match-and-replace rules in the "Match and Replace Rules" section.
   - Click the "Save Profile" button to save your profile.

2. **Selecting and Applying a Profile:**
   - Select a profile from the list on the left.
   - The selected profile’s headers and match rules will be displayed on the right.
   - Apply the profile to your requests using the "Apply Profile" option.

3. **Match and Replace Rules:**
   - Enter a regex pattern in the "Match (Regex)" column.
   - Specify how the matched regex pattern should be replaced in the "Replace" column.
   - Use the "Add Rule" button to create new rules, and the "Remove Rule" button to delete existing ones.

4. **Deleting a Profile:**
   - Select the profile you want to delete and click the "Delete Profile" button.

## Requirements
- Burp Suite Pro or Free Edition
- Java 1.8 or above

## License
This project is licensed under the MIT License.
