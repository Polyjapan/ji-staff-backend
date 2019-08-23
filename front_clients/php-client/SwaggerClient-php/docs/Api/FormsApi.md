# Swagger\Client\FormsApi

All URIs are relative to *https://staff.japan-impact.ch/api/v2/front*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getCurrentForms**](FormsApi.md#getCurrentForms) | **GET** /forms | Returns all the forms of the currently active events
[**getCurrentMainForm**](FormsApi.md#getCurrentMainForm) | **GET** /forms/main_form | Returns the main form of the current edition
[**getFormById**](FormsApi.md#getFormById) | **GET** /forms/{id} | Returns the requested form, regardless of whether the edition is active or not
[**getFormPageByIdAndPosition**](FormsApi.md#getFormPageByIdAndPosition) | **GET** /forms/{id}/pages/{pageNum} | Returns a specific page in the form
[**getFormPagesById**](FormsApi.md#getFormPagesById) | **GET** /forms/{id}/pages | Returns the list of pages of a given form


# **getCurrentForms**
> \Swagger\Client\Model\Form[] getCurrentForms()

Returns all the forms of the currently active events

Returns an array of forms, of which some may be closed

### Example
```php
<?php
require_once(__DIR__ . '/vendor/autoload.php');

$apiInstance = new Swagger\Client\Api\FormsApi(
    // If you want use custom http client, pass your client which implements `GuzzleHttp\ClientInterface`.
    // This is optional, `GuzzleHttp\Client` will be used as default.
    new GuzzleHttp\Client()
);

try {
    $result = $apiInstance->getCurrentForms();
    print_r($result);
} catch (Exception $e) {
    echo 'Exception when calling FormsApi->getCurrentForms: ', $e->getMessage(), PHP_EOL;
}
?>
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**\Swagger\Client\Model\Form[]**](../Model/Form.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../../README.md#documentation-for-api-endpoints) [[Back to Model list]](../../README.md#documentation-for-models) [[Back to README]](../../README.md)

# **getCurrentMainForm**
> \Swagger\Client\Model\Form getCurrentMainForm()

Returns the main form of the current edition



### Example
```php
<?php
require_once(__DIR__ . '/vendor/autoload.php');

$apiInstance = new Swagger\Client\Api\FormsApi(
    // If you want use custom http client, pass your client which implements `GuzzleHttp\ClientInterface`.
    // This is optional, `GuzzleHttp\Client` will be used as default.
    new GuzzleHttp\Client()
);

try {
    $result = $apiInstance->getCurrentMainForm();
    print_r($result);
} catch (Exception $e) {
    echo 'Exception when calling FormsApi->getCurrentMainForm: ', $e->getMessage(), PHP_EOL;
}
?>
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**\Swagger\Client\Model\Form**](../Model/Form.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../../README.md#documentation-for-api-endpoints) [[Back to Model list]](../../README.md#documentation-for-models) [[Back to README]](../../README.md)

# **getFormById**
> \Swagger\Client\Model\Form getFormById($id)

Returns the requested form, regardless of whether the edition is active or not



### Example
```php
<?php
require_once(__DIR__ . '/vendor/autoload.php');

$apiInstance = new Swagger\Client\Api\FormsApi(
    // If you want use custom http client, pass your client which implements `GuzzleHttp\ClientInterface`.
    // This is optional, `GuzzleHttp\Client` will be used as default.
    new GuzzleHttp\Client()
);
$id = 56; // int | ID of form to return

try {
    $result = $apiInstance->getFormById($id);
    print_r($result);
} catch (Exception $e) {
    echo 'Exception when calling FormsApi->getFormById: ', $e->getMessage(), PHP_EOL;
}
?>
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **int**| ID of form to return |

### Return type

[**\Swagger\Client\Model\Form**](../Model/Form.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../../README.md#documentation-for-api-endpoints) [[Back to Model list]](../../README.md#documentation-for-models) [[Back to README]](../../README.md)

# **getFormPageByIdAndPosition**
> \Swagger\Client\Model\InlineResponse200 getFormPageByIdAndPosition($id, $page_num)

Returns a specific page in the form



### Example
```php
<?php
require_once(__DIR__ . '/vendor/autoload.php');

$apiInstance = new Swagger\Client\Api\FormsApi(
    // If you want use custom http client, pass your client which implements `GuzzleHttp\ClientInterface`.
    // This is optional, `GuzzleHttp\Client` will be used as default.
    new GuzzleHttp\Client()
);
$id = 56; // int | ID of form to look for
$page_num = 56; // int | the position of the page, starting at 1 and ending at the total number of pages

try {
    $result = $apiInstance->getFormPageByIdAndPosition($id, $page_num);
    print_r($result);
} catch (Exception $e) {
    echo 'Exception when calling FormsApi->getFormPageByIdAndPosition: ', $e->getMessage(), PHP_EOL;
}
?>
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **int**| ID of form to look for |
 **page_num** | **int**| the position of the page, starting at 1 and ending at the total number of pages |

### Return type

[**\Swagger\Client\Model\InlineResponse200**](../Model/InlineResponse200.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../../README.md#documentation-for-api-endpoints) [[Back to Model list]](../../README.md#documentation-for-models) [[Back to README]](../../README.md)

# **getFormPagesById**
> \Swagger\Client\Model\FormPage[] getFormPagesById($id)

Returns the list of pages of a given form



### Example
```php
<?php
require_once(__DIR__ . '/vendor/autoload.php');

$apiInstance = new Swagger\Client\Api\FormsApi(
    // If you want use custom http client, pass your client which implements `GuzzleHttp\ClientInterface`.
    // This is optional, `GuzzleHttp\Client` will be used as default.
    new GuzzleHttp\Client()
);
$id = 56; // int | ID of form to look for

try {
    $result = $apiInstance->getFormPagesById($id);
    print_r($result);
} catch (Exception $e) {
    echo 'Exception when calling FormsApi->getFormPagesById: ', $e->getMessage(), PHP_EOL;
}
?>
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **int**| ID of form to look for |

### Return type

[**\Swagger\Client\Model\FormPage[]**](../Model/FormPage.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../../README.md#documentation-for-api-endpoints) [[Back to Model list]](../../README.md#documentation-for-models) [[Back to README]](../../README.md)

