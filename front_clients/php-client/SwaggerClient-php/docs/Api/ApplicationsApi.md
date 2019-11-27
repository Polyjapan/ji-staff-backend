# Swagger\Client\ApplicationsApi

All URIs are relative to *https://staff.japan-impact.ch/api/v2/front*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getApplicationComments**](ApplicationsApi.md#getApplicationComments) | **GET** /applications/{formId}/comments/{userId} | Returns all the public comments made on this application
[**getApplicationReplies**](ApplicationsApi.md#getApplicationReplies) | **GET** /applications/{formId}/replies/{userId} | Returns the current content of an application
[**getApplicationState**](ApplicationsApi.md#getApplicationState) | **GET** /applications/{formId}/state/{userId} | Returns the state of an application
[**updateApplicationReplies**](ApplicationsApi.md#updateApplicationReplies) | **POST** /applications/{formId}/replies/{userId} | Sends some replies to add to the application.
[**updateApplicationState**](ApplicationsApi.md#updateApplicationState) | **PUT** /applications/{formId}/state/{userId} | Updates the state of an application


# **getApplicationComments**
> \Swagger\Client\Model\InlineResponse2003[] getApplicationComments($form_id, $user_id)

Returns all the public comments made on this application

These comments are usefull for the user and should be displayed to it. They allow the admins to provide a reason when an application is refused, of when changes are requested for example.

### Example
```php
<?php
require_once(__DIR__ . '/vendor/autoload.php');

// Configure API key authorization: api_key
$config = Swagger\Client\Configuration::getDefaultConfiguration()->setApiKey('Authorization', 'YOUR_API_KEY');
// Uncomment below to setup prefix (e.g. Bearer) for API key, if needed
// $config = Swagger\Client\Configuration::getDefaultConfiguration()->setApiKeyPrefix('Authorization', 'Bearer');

$apiInstance = new Swagger\Client\Api\ApplicationsApi(
    // If you want use custom http client, pass your client which implements `GuzzleHttp\ClientInterface`.
    // This is optional, `GuzzleHttp\Client` will be used as default.
    new GuzzleHttp\Client(),
    $config
);
$form_id = 56; // int | ID of the form the user is applying on
$user_id = 56; // int | ID of the user who is applying

try {
    $result = $apiInstance->getApplicationComments($form_id, $user_id);
    print_r($result);
} catch (Exception $e) {
    echo 'Exception when calling ApplicationsApi->getApplicationComments: ', $e->getMessage(), PHP_EOL;
}
?>
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **form_id** | **int**| ID of the form the user is applying on |
 **user_id** | **int**| ID of the user who is applying |

### Return type

[**\Swagger\Client\Model\InlineResponse2003[]**](../Model/InlineResponse2003.md)

### Authorization

[api_key](../../README.md#api_key)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../../README.md#documentation-for-api-endpoints) [[Back to Model list]](../../README.md#documentation-for-models) [[Back to README]](../../README.md)

# **getApplicationReplies**
> \Swagger\Client\Model\ApplicationReply[] getApplicationReplies($form_id, $user_id)

Returns the current content of an application



### Example
```php
<?php
require_once(__DIR__ . '/vendor/autoload.php');

// Configure API key authorization: api_key
$config = Swagger\Client\Configuration::getDefaultConfiguration()->setApiKey('Authorization', 'YOUR_API_KEY');
// Uncomment below to setup prefix (e.g. Bearer) for API key, if needed
// $config = Swagger\Client\Configuration::getDefaultConfiguration()->setApiKeyPrefix('Authorization', 'Bearer');

$apiInstance = new Swagger\Client\Api\ApplicationsApi(
    // If you want use custom http client, pass your client which implements `GuzzleHttp\ClientInterface`.
    // This is optional, `GuzzleHttp\Client` will be used as default.
    new GuzzleHttp\Client(),
    $config
);
$form_id = 56; // int | ID of the form the user is applying on
$user_id = 56; // int | ID of the user who is applying

try {
    $result = $apiInstance->getApplicationReplies($form_id, $user_id);
    print_r($result);
} catch (Exception $e) {
    echo 'Exception when calling ApplicationsApi->getApplicationReplies: ', $e->getMessage(), PHP_EOL;
}
?>
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **form_id** | **int**| ID of the form the user is applying on |
 **user_id** | **int**| ID of the user who is applying |

### Return type

[**\Swagger\Client\Model\ApplicationReply[]**](../Model/ApplicationReply.md)

### Authorization

[api_key](../../README.md#api_key)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../../README.md#documentation-for-api-endpoints) [[Back to Model list]](../../README.md#documentation-for-models) [[Back to README]](../../README.md)

# **getApplicationState**
> \Swagger\Client\Model\ApplicationState getApplicationState($form_id, $user_id)

Returns the state of an application



### Example
```php
<?php
require_once(__DIR__ . '/vendor/autoload.php');

// Configure API key authorization: api_key
$config = Swagger\Client\Configuration::getDefaultConfiguration()->setApiKey('Authorization', 'YOUR_API_KEY');
// Uncomment below to setup prefix (e.g. Bearer) for API key, if needed
// $config = Swagger\Client\Configuration::getDefaultConfiguration()->setApiKeyPrefix('Authorization', 'Bearer');

$apiInstance = new Swagger\Client\Api\ApplicationsApi(
    // If you want use custom http client, pass your client which implements `GuzzleHttp\ClientInterface`.
    // This is optional, `GuzzleHttp\Client` will be used as default.
    new GuzzleHttp\Client(),
    $config
);
$form_id = 56; // int | ID of the form the user is applying on
$user_id = 56; // int | ID of the user who is applying

try {
    $result = $apiInstance->getApplicationState($form_id, $user_id);
    print_r($result);
} catch (Exception $e) {
    echo 'Exception when calling ApplicationsApi->getApplicationState: ', $e->getMessage(), PHP_EOL;
}
?>
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **form_id** | **int**| ID of the form the user is applying on |
 **user_id** | **int**| ID of the user who is applying |

### Return type

[**\Swagger\Client\Model\ApplicationState**](../Model/ApplicationState.md)

### Authorization

[api_key](../../README.md#api_key)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../../README.md#documentation-for-api-endpoints) [[Back to Model list]](../../README.md#documentation-for-models) [[Back to README]](../../README.md)

# **updateApplicationReplies**
> updateApplicationReplies($form_id, $user_id, $body)

Sends some replies to add to the application.

Not all replies have to be sent at once, you can send them one by one.<br>If a reply for a given field has already been sent, it will be updated.<br>If no application exist for this form and user, it will be created.<br>Replies are expected to have been checked before being sent.

### Example
```php
<?php
require_once(__DIR__ . '/vendor/autoload.php');

// Configure API key authorization: api_key
$config = Swagger\Client\Configuration::getDefaultConfiguration()->setApiKey('Authorization', 'YOUR_API_KEY');
// Uncomment below to setup prefix (e.g. Bearer) for API key, if needed
// $config = Swagger\Client\Configuration::getDefaultConfiguration()->setApiKeyPrefix('Authorization', 'Bearer');

$apiInstance = new Swagger\Client\Api\ApplicationsApi(
    // If you want use custom http client, pass your client which implements `GuzzleHttp\ClientInterface`.
    // This is optional, `GuzzleHttp\Client` will be used as default.
    new GuzzleHttp\Client(),
    $config
);
$form_id = 56; // int | ID of the form the user is applying on
$user_id = 56; // int | ID of the user who is applying
$body = array(new \Swagger\Client\Model\ApplicationReply()); // \Swagger\Client\Model\ApplicationReply[] | The fields to add or update in the application

try {
    $apiInstance->updateApplicationReplies($form_id, $user_id, $body);
} catch (Exception $e) {
    echo 'Exception when calling ApplicationsApi->updateApplicationReplies: ', $e->getMessage(), PHP_EOL;
}
?>
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **form_id** | **int**| ID of the form the user is applying on |
 **user_id** | **int**| ID of the user who is applying |
 **body** | [**\Swagger\Client\Model\ApplicationReply[]**](../Model/ApplicationReply.md)| The fields to add or update in the application |

### Return type

void (empty response body)

### Authorization

[api_key](../../README.md#api_key)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../../README.md#documentation-for-api-endpoints) [[Back to Model list]](../../README.md#documentation-for-models) [[Back to README]](../../README.md)

# **updateApplicationState**
> updateApplicationState($form_id, $user_id, $body)

Updates the state of an application

If the application doesn't exist and the given state is `draft`, it will be created.

### Example
```php
<?php
require_once(__DIR__ . '/vendor/autoload.php');

// Configure API key authorization: api_key
$config = Swagger\Client\Configuration::getDefaultConfiguration()->setApiKey('Authorization', 'YOUR_API_KEY');
// Uncomment below to setup prefix (e.g. Bearer) for API key, if needed
// $config = Swagger\Client\Configuration::getDefaultConfiguration()->setApiKeyPrefix('Authorization', 'Bearer');

$apiInstance = new Swagger\Client\Api\ApplicationsApi(
    // If you want use custom http client, pass your client which implements `GuzzleHttp\ClientInterface`.
    // This is optional, `GuzzleHttp\Client` will be used as default.
    new GuzzleHttp\Client(),
    $config
);
$form_id = 56; // int | ID of the form the user is applying on
$user_id = 56; // int | ID of the user who is applying
$body = new \Swagger\Client\Model\ApplicationState(); // \Swagger\Client\Model\ApplicationState | The new state of the application

try {
    $apiInstance->updateApplicationState($form_id, $user_id, $body);
} catch (Exception $e) {
    echo 'Exception when calling ApplicationsApi->updateApplicationState: ', $e->getMessage(), PHP_EOL;
}
?>
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **form_id** | **int**| ID of the form the user is applying on |
 **user_id** | **int**| ID of the user who is applying |
 **body** | [**\Swagger\Client\Model\ApplicationState**](../Model/ApplicationState.md)| The new state of the application |

### Return type

void (empty response body)

### Authorization

[api_key](../../README.md#api_key)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../../README.md#documentation-for-api-endpoints) [[Back to Model list]](../../README.md#documentation-for-models) [[Back to README]](../../README.md)

