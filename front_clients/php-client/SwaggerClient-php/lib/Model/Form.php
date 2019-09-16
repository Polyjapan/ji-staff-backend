<?php
/**
 * Form
 *
 * PHP version 5
 *
 * @category Class
 * @package  Swagger\Client
 * @author   Swagger Codegen team
 * @link     https://github.com/swagger-api/swagger-codegen
 */

/**
 * Japan Impact Staffs
 *
 * This is the documentation for the front-office API of the new staffs infrastructure.
 *
 * OpenAPI spec version: 1.0.0
 * 
 * Generated by: https://github.com/swagger-api/swagger-codegen.git
 * Swagger Codegen version: 2.4.8
 */

/**
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen
 * Do not edit the class manually.
 */

namespace Swagger\Client\Model;

use \ArrayAccess;
use \Swagger\Client\ObjectSerializer;

/**
 * Form Class Doc Comment
 *
 * @category Class
 * @package  Swagger\Client
 * @author   Swagger Codegen team
 * @link     https://github.com/swagger-api/swagger-codegen
 */
class Form implements ModelInterface, ArrayAccess
{
    const DISCRIMINATOR = null;

    /**
      * The original name of the model.
      *
      * @var string
      */
    protected static $swaggerModelName = 'Form';

    /**
      * Array of property to type mappings. Used for (de)serialization
      *
      * @var string[]
      */
    protected static $swaggerTypes = [
        'form_id' => 'int',
        'event_id' => 'int',
        'internal_name' => 'string',
        'name' => 'string',
        'short_description' => 'string',
        'description' => 'string',
        'max_age' => 'int',
        'min_age' => 'int',
        'requires_staff' => 'bool',
        'hidden' => 'bool',
        'close_date' => 'int'
    ];

    /**
      * Array of property to format mappings. Used for (de)serialization
      *
      * @var string[]
      */
    protected static $swaggerFormats = [
        'form_id' => 'int32',
        'event_id' => 'int32',
        'internal_name' => null,
        'name' => null,
        'short_description' => null,
        'description' => null,
        'max_age' => 'int32',
        'min_age' => 'int32',
        'requires_staff' => null,
        'hidden' => null,
        'close_date' => 'int64'
    ];

    /**
     * Array of property to type mappings. Used for (de)serialization
     *
     * @return array
     */
    public static function swaggerTypes()
    {
        return self::$swaggerTypes;
    }

    /**
     * Array of property to format mappings. Used for (de)serialization
     *
     * @return array
     */
    public static function swaggerFormats()
    {
        return self::$swaggerFormats;
    }

    /**
     * Array of attributes where the key is the local name,
     * and the value is the original name
     *
     * @var string[]
     */
    protected static $attributeMap = [
        'form_id' => 'formId',
        'event_id' => 'eventId',
        'internal_name' => 'internalName',
        'name' => 'name',
        'short_description' => 'shortDescription',
        'description' => 'description',
        'max_age' => 'maxAge',
        'min_age' => 'minAge',
        'requires_staff' => 'requiresStaff',
        'hidden' => 'hidden',
        'close_date' => 'closeDate'
    ];

    /**
     * Array of attributes to setter functions (for deserialization of responses)
     *
     * @var string[]
     */
    protected static $setters = [
        'form_id' => 'setFormId',
        'event_id' => 'setEventId',
        'internal_name' => 'setInternalName',
        'name' => 'setName',
        'short_description' => 'setShortDescription',
        'description' => 'setDescription',
        'max_age' => 'setMaxAge',
        'min_age' => 'setMinAge',
        'requires_staff' => 'setRequiresStaff',
        'hidden' => 'setHidden',
        'close_date' => 'setCloseDate'
    ];

    /**
     * Array of attributes to getter functions (for serialization of requests)
     *
     * @var string[]
     */
    protected static $getters = [
        'form_id' => 'getFormId',
        'event_id' => 'getEventId',
        'internal_name' => 'getInternalName',
        'name' => 'getName',
        'short_description' => 'getShortDescription',
        'description' => 'getDescription',
        'max_age' => 'getMaxAge',
        'min_age' => 'getMinAge',
        'requires_staff' => 'getRequiresStaff',
        'hidden' => 'getHidden',
        'close_date' => 'getCloseDate'
    ];

    /**
     * Array of attributes where the key is the local name,
     * and the value is the original name
     *
     * @return array
     */
    public static function attributeMap()
    {
        return self::$attributeMap;
    }

    /**
     * Array of attributes to setter functions (for deserialization of responses)
     *
     * @return array
     */
    public static function setters()
    {
        return self::$setters;
    }

    /**
     * Array of attributes to getter functions (for serialization of requests)
     *
     * @return array
     */
    public static function getters()
    {
        return self::$getters;
    }

    /**
     * The original name of the model.
     *
     * @return string
     */
    public function getModelName()
    {
        return self::$swaggerModelName;
    }

    

    

    /**
     * Associative array for storing property values
     *
     * @var mixed[]
     */
    protected $container = [];

    /**
     * Constructor
     *
     * @param mixed[] $data Associated array of property values
     *                      initializing the model
     */
    public function __construct(array $data = null)
    {
        $this->container['form_id'] = isset($data['form_id']) ? $data['form_id'] : null;
        $this->container['event_id'] = isset($data['event_id']) ? $data['event_id'] : null;
        $this->container['internal_name'] = isset($data['internal_name']) ? $data['internal_name'] : null;
        $this->container['name'] = isset($data['name']) ? $data['name'] : null;
        $this->container['short_description'] = isset($data['short_description']) ? $data['short_description'] : null;
        $this->container['description'] = isset($data['description']) ? $data['description'] : null;
        $this->container['max_age'] = isset($data['max_age']) ? $data['max_age'] : null;
        $this->container['min_age'] = isset($data['min_age']) ? $data['min_age'] : null;
        $this->container['requires_staff'] = isset($data['requires_staff']) ? $data['requires_staff'] : null;
        $this->container['hidden'] = isset($data['hidden']) ? $data['hidden'] : null;
        $this->container['close_date'] = isset($data['close_date']) ? $data['close_date'] : null;
    }

    /**
     * Show all the invalid properties with reasons.
     *
     * @return array invalid properties with reasons
     */
    public function listInvalidProperties()
    {
        $invalidProperties = [];

        return $invalidProperties;
    }

    /**
     * Validate all the properties in the model
     * return true if all passed
     *
     * @return bool True if all properties are valid
     */
    public function valid()
    {
        return count($this->listInvalidProperties()) === 0;
    }


    /**
     * Gets form_id
     *
     * @return int
     */
    public function getFormId()
    {
        return $this->container['form_id'];
    }

    /**
     * Sets form_id
     *
     * @param int $form_id form_id
     *
     * @return $this
     */
    public function setFormId($form_id)
    {
        $this->container['form_id'] = $form_id;

        return $this;
    }

    /**
     * Gets event_id
     *
     * @return int
     */
    public function getEventId()
    {
        return $this->container['event_id'];
    }

    /**
     * Sets event_id
     *
     * @param int $event_id event_id
     *
     * @return $this
     */
    public function setEventId($event_id)
    {
        $this->container['event_id'] = $event_id;

        return $this;
    }

    /**
     * Gets internal_name
     *
     * @return string
     */
    public function getInternalName()
    {
        return $this->container['internal_name'];
    }

    /**
     * Sets internal_name
     *
     * @param string $internal_name an internal name for this form, for use in URLs for example
     *
     * @return $this
     */
    public function setInternalName($internal_name)
    {
        $this->container['internal_name'] = $internal_name;

        return $this;
    }

    /**
     * Gets name
     *
     * @return string
     */
    public function getName()
    {
        return $this->container['name'];
    }

    /**
     * Sets name
     *
     * @param string $name the public displayable name for this form
     *
     * @return $this
     */
    public function setName($name)
    {
        $this->container['name'] = $name;

        return $this;
    }

    /**
     * Gets short_description
     *
     * @return string
     */
    public function getShortDescription()
    {
        return $this->container['short_description'];
    }

    /**
     * Sets short_description
     *
     * @param string $short_description a short description of this form, to display in a list of forms for example
     *
     * @return $this
     */
    public function setShortDescription($short_description)
    {
        $this->container['short_description'] = $short_description;

        return $this;
    }

    /**
     * Gets description
     *
     * @return string
     */
    public function getDescription()
    {
        return $this->container['description'];
    }

    /**
     * Sets description
     *
     * @param string $description a longer description of this form, to display in a pre-reply page for example
     *
     * @return $this
     */
    public function setDescription($description)
    {
        $this->container['description'] = $description;

        return $this;
    }

    /**
     * Gets max_age
     *
     * @return int
     */
    public function getMaxAge()
    {
        return $this->container['max_age'];
    }

    /**
     * Sets max_age
     *
     * @param int $max_age the maximal age to see this form, the age being computed at the event's eventBegin date, can be set to -1 to remove limit
     *
     * @return $this
     */
    public function setMaxAge($max_age)
    {
        $this->container['max_age'] = $max_age;

        return $this;
    }

    /**
     * Gets min_age
     *
     * @return int
     */
    public function getMinAge()
    {
        return $this->container['min_age'];
    }

    /**
     * Sets min_age
     *
     * @param int $min_age the minimal age to see this form, the age being computed at the event's eventBegin date, can be set to -1 to remove limit
     *
     * @return $this
     */
    public function setMinAge($min_age)
    {
        $this->container['min_age'] = $min_age;

        return $this;
    }

    /**
     * Gets requires_staff
     *
     * @return bool
     */
    public function getRequiresStaff()
    {
        return $this->container['requires_staff'];
    }

    /**
     * Sets requires_staff
     *
     * @param bool $requires_staff if true, this form can only be seen by people who have been accepted as staff
     *
     * @return $this
     */
    public function setRequiresStaff($requires_staff)
    {
        $this->container['requires_staff'] = $requires_staff;

        return $this;
    }

    /**
     * Gets hidden
     *
     * @return bool
     */
    public function getHidden()
    {
        return $this->container['hidden'];
    }

    /**
     * Sets hidden
     *
     * @param bool $hidden if true, this form cannot be seen by users. users who already replied MAY me able to still see it, but MAY NOT update their answers
     *
     * @return $this
     */
    public function setHidden($hidden)
    {
        $this->container['hidden'] = $hidden;

        return $this;
    }

    /**
     * Gets close_date
     *
     * @return int
     */
    public function getCloseDate()
    {
        return $this->container['close_date'];
    }

    /**
     * Sets close_date
     *
     * @param int $close_date the Timestamp in milliseconds at which this form MUST stop accepting new answers (can be null if unlimited)
     *
     * @return $this
     */
    public function setCloseDate($close_date)
    {
        $this->container['close_date'] = $close_date;

        return $this;
    }
    /**
     * Returns true if offset exists. False otherwise.
     *
     * @param integer $offset Offset
     *
     * @return boolean
     */
    public function offsetExists($offset)
    {
        return isset($this->container[$offset]);
    }

    /**
     * Gets offset.
     *
     * @param integer $offset Offset
     *
     * @return mixed
     */
    public function offsetGet($offset)
    {
        return isset($this->container[$offset]) ? $this->container[$offset] : null;
    }

    /**
     * Sets value based on offset.
     *
     * @param integer $offset Offset
     * @param mixed   $value  Value to be set
     *
     * @return void
     */
    public function offsetSet($offset, $value)
    {
        if (is_null($offset)) {
            $this->container[] = $value;
        } else {
            $this->container[$offset] = $value;
        }
    }

    /**
     * Unsets offset.
     *
     * @param integer $offset Offset
     *
     * @return void
     */
    public function offsetUnset($offset)
    {
        unset($this->container[$offset]);
    }

    /**
     * Gets the string presentation of the object
     *
     * @return string
     */
    public function __toString()
    {
        if (defined('JSON_PRETTY_PRINT')) { // use JSON pretty print
            return json_encode(
                ObjectSerializer::sanitizeForSerialization($this),
                JSON_PRETTY_PRINT
            );
        }

        return json_encode(ObjectSerializer::sanitizeForSerialization($this));
    }
}


