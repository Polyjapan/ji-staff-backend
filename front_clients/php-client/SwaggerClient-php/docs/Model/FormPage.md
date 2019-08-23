# FormPage

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**page_id** | **int** |  | [optional] 
**form_id** | **int** |  | [optional] 
**name** | **string** | the public displayable name for this page | [optional] 
**description** | **string** | the description of this page, to display in the top of the page for example | [optional] 
**max_age** | **int** | the maximal age to see this page, the age being computed at the event&#39;s eventBegin date, can be set to -1 to remove limit | [optional] 
**min_age** | **int** | the minimal age to see this page, the age being computed at the event&#39;s eventBegin date, can be set to -1 to remove limit | [optional] 
**ordering** | **int** | the order of this page, used to sort the pages in order (smallest is first, null is always smaller). If multiple pages have same ordering, sort using pageId. | [optional] 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


