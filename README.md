# Large Model Filing Information Submission System

A Vue 3-based system for submitting large model filing information, including an **Admin Portal** and a **User Portal**.

## System Architecture

 **Admin Portal**

  * Template Management
  * User Management
  * Task Dashboard
  * Change Password
  * Logout

 **User Portal**

  * Filing Application
  * Filing Center
  * Change Password
  * Logout

### Project Setup

```sh
npm install
```

### Development Mode

```sh
npm run dev
```

### Production Build

```sh
npm run build
```

## Project Structure

```
src/
├── assets/          # Static resources
├── views/           # Page components
│   ├── admin/       # Admin portal pages
│   ├── user/        # User portal pages
│   └── common/      # Shared pages
├── router/          # Routing configuration
├── App.vue          # Root component
└── main.js          # Entry point
```

### Linting with [ESLint](https://eslint.org/)

```sh
npm run lint
```

---

# Configuring multipart/form-data Requests in Talend API Tester

## Basic Steps

1. **Open Talend API Tester**

   * Click the Talend API Tester extension icon in Chrome

2. **Create a New Request**

   * Click the "+" button to create a new request
   * Enter a name like `Save Template`

3. **Set Request Method and URL**

   * Method: `POST`
   * URL:

     ```
     http://your-api-host/api/templateRegistry/saveTemplateRegistry
     ```

4. **Add Authorization Header**

   * In the “Headers” tab, click `Add header`
   * Name: `Authorization`
   * Value: `Bearer your_token_here` (replace with your actual token)

## Configure multipart/form-data

5. **Select Correct Content Type**

   * In the "Body" tab, choose `form-data` (not `raw` or `x-www-form-urlencoded`)

6. **Add File Parameter**

   * Click `Add parameter`
   * Name: `file`
   * Type: File (click the file icon)
   * Upload a Word template file via `Choose file`

7. **Add JSON Data Parameter**

   * Click `Add parameter`
   * Name: `data`
   * Type: Text
   * Value: A valid JSON string (double-quoted keys and values)
   * Example:

     ```json
     {
       "templateName": "Enterprise Filing Application",
       "templateCode": "TPL-2023001",
       "templateDescription": "Template for enterprise filing",
       "templateType": "Enterprise Filing",
       "templateContent": "{\"sections\":[{\"sectionTitle\":\"Basic Info\",\"fields\":[{\"id\":\"name\",\"label\":\"Enterprise Name\",\"type\":\"text\"}]}]}"
     }
     ```

## Common Error Prevention

1. **Ensure Valid JSON Format**

   * All keys and string values must use double quotes
   * No trailing commas
   * The `templateContent` must be a valid stringified JSON

2. **Do Not Manually Set multipart Boundaries**

   * Talend handles `multipart/form-data` boundaries automatically

3. **Verify Parameter Names**

   * Parameters must be exactly named `file` and `data` (case-sensitive)

## Sending the Request

8. **Send the Request**

   * Click the `Send` button
   * Check the status code and response

9. **Inspect Request Details**

   * After sending, check the “Request” tab to verify the full request
   * Ensure the `Content-Type` includes `multipart/form-data; boundary=...`

### If You Encounter `"templateName cannot be empty"` Error:

* Ensure the `data` parameter contains the `templateName` field
* Verify the JSON is 100% valid
* Do **not** mislabel the `data` parameter (e.g., using `"formData"`)
