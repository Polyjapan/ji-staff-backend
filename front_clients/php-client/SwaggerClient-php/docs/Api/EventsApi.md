# Swagger\Client\EventsApi

All URIs are relative to *https://staff.japan-impact.ch/api/v2/front*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getCurrentEdition**](EventsApi.md#getCurrentEdition) | **GET** /edition | Returns the current active event


# **getCurrentEdition**
> \Swagger\Client\Model\Event getCurrentEdition()

Returns the current active event

Returns a single event

### Example
```php
<?php
require_once(__DIR__ . '/vendor/autoload.php');

$apiInstance = new Swagger\Client\Api\EventsApi(
    // If you want use custom http client, pass your client which implements `GuzzleHttp\ClientInterface`.
    // This is optional, `GuzzleHttp\Client` will be used as default.
    new GuzzleHttp\Client()
);

try {
    $result = $apiInstance->getCurrentEdition();
    print_r($result);
} catch (Exception $e) {
    echo 'Exception when calling EventsApi->getCurrentEdition: ', $e->getMessage(), PHP_EOL;
}
?>
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**\Swagger\Client\Model\Event**](../Model/Event.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../../README.md#documentation-for-api-endpoints) [[Back to Model list]](../../README.md#documentation-for-models) [[Back to README]](../../README.md)

