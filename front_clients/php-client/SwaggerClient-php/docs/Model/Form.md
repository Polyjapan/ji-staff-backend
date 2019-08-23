# Form

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**form_id** | **int** |  | [optional] 
**event_id** | **int** |  | [optional] 
**internal_name** | **string** | an internal name for this form, for use in URLs for example | [optional] 
**name** | **string** | the public displayable name for this form | [optional] 
**short_description** | **string** | a short description of this form, to display in a list of forms for example | [optional] 
**description** | **string** | a longer description of this form, to display in a pre-reply page for example | [optional] 
**max_age** | **int** | the maximal age to see this form, the age being computed at the event&#39;s eventBegin date, can be set to -1 to remove limit | [optional] 
**min_age** | **int** | the minimal age to see this form, the age being computed at the event&#39;s eventBegin date, can be set to -1 to remove limit | [optional] 
**requires_staff** | **bool** | if true, this form can only be seen by people who have been accepted as staff | [optional] 
**hidden** | **bool** | if true, this form cannot be seen by users. users who already replied MAY me able to still see it, but MAY NOT update their answers | [optional] 
**close_date** | **int** | the Timestamp in milliseconds at which this form MUST stop accepting new answers (can be null if unlimited) | [optional] 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


