<?php
/**
 * FormField
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
 * FormField Class Doc Comment
 *
 * @category Class
 * @package  Swagger\Client
 * @author   Swagger Codegen team
 * @link     https://github.com/swagger-api/swagger-codegen
 */
class FormField implements ModelInterface, ArrayAccess
{
    const DISCRIMINATOR = null;

    /**
      * The original name of the model.
      *
      * @var string
      */
    protected static $swaggerModelName = 'FormField';

    /**
      * Array of property to type mappings. Used for (de)serialization
      *
      * @var string[]
      */
    protected static $swaggerTypes = [
        'field_id' => 'int',
        'page_id' => 'int',
        'name' => 'string',
        'label' => 'string',
        'help_text' => 'string',
        'type' => 'string',
        'required' => 'bool',
        'ordering' => 'int'
    ];

    /**
      * Array of property to format mappings. Used for (de)serialization
      *
      * @var string[]
      */
    protected static $swaggerFormats = [
        'field_id' => 'int32',
        'page_id' => 'int32',
        'name' => null,
        'label' => null,
        'help_text' => null,
        'type' => null,
        'required' => null,
        'ordering' => 'int32'
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
        'field_id' => 'fieldId',
        'page_id' => 'pageId',
        'name' => 'name',
        'label' => 'label',
        'help_text' => 'helpText',
        'type' => 'type',
        'required' => 'required',
        'ordering' => 'ordering'
    ];

    /**
     * Array of attributes to setter functions (for deserialization of responses)
     *
     * @var string[]
     */
    protected static $setters = [
        'field_id' => 'setFieldId',
        'page_id' => 'setPageId',
        'name' => 'setName',
        'label' => 'setLabel',
        'help_text' => 'setHelpText',
        'type' => 'setType',
        'required' => 'setRequired',
        'ordering' => 'setOrdering'
    ];

    /**
     * Array of attributes to getter functions (for serialization of requests)
     *
     * @var string[]
     */
    protected static $getters = [
        'field_id' => 'getFieldId',
        'page_id' => 'getPageId',
        'name' => 'getName',
        'label' => 'getLabel',
        'help_text' => 'getHelpText',
        'type' => 'getType',
        'required' => 'getRequired',
        'ordering' => 'getOrdering'
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

    const TYPE_TEXT = 'text';
    const TYPE_LONG_TEXT = 'long_text';
    const TYPE_EMAIL = 'email';
    const TYPE_DATE = 'date';
    const TYPE_CHECKBOX = 'checkbox';
    const TYPE_SELECT = 'select';
    const TYPE_FILE = 'file';
    const TYPE_IMAGE = 'image';
    const TYPE_URL = 'url';
    

    
    /**
     * Gets allowable values of the enum
     *
     * @return string[]
     */
    public function getTypeAllowableValues()
    {
        return [
            self::TYPE_TEXT,
            self::TYPE_LONG_TEXT,
            self::TYPE_EMAIL,
            self::TYPE_DATE,
            self::TYPE_CHECKBOX,
            self::TYPE_SELECT,
            self::TYPE_FILE,
            self::TYPE_IMAGE,
            self::TYPE_URL,
        ];
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
        $this->container['field_id'] = isset($data['field_id']) ? $data['field_id'] : null;
        $this->container['page_id'] = isset($data['page_id']) ? $data['page_id'] : null;
        $this->container['name'] = isset($data['name']) ? $data['name'] : null;
        $this->container['label'] = isset($data['label']) ? $data['label'] : null;
        $this->container['help_text'] = isset($data['help_text']) ? $data['help_text'] : null;
        $this->container['type'] = isset($data['type']) ? $data['type'] : null;
        $this->container['required'] = isset($data['required']) ? $data['required'] : null;
        $this->container['ordering'] = isset($data['ordering']) ? $data['ordering'] : null;
    }

    /**
     * Show all the invalid properties with reasons.
     *
     * @return array invalid properties with reasons
     */
    public function listInvalidProperties()
    {
        $invalidProperties = [];

        $allowedValues = $this->getTypeAllowableValues();
        if (!is_null($this->container['type']) && !in_array($this->container['type'], $allowedValues, true)) {
            $invalidProperties[] = sprintf(
                "invalid value for 'type', must be one of '%s'",
                implode("', '", $allowedValues)
            );
        }

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
     * Gets field_id
     *
     * @return int
     */
    public function getFieldId()
    {
        return $this->container['field_id'];
    }

    /**
     * Sets field_id
     *
     * @param int $field_id field_id
     *
     * @return $this
     */
    public function setFieldId($field_id)
    {
        $this->container['field_id'] = $field_id;

        return $this;
    }

    /**
     * Gets page_id
     *
     * @return int
     */
    public function getPageId()
    {
        return $this->container['page_id'];
    }

    /**
     * Sets page_id
     *
     * @param int $page_id page_id
     *
     * @return $this
     */
    public function setPageId($page_id)
    {
        $this->container['page_id'] = $page_id;

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
     * @param string $name a name, that could be used for HTML generation or to display to the user
     *
     * @return $this
     */
    public function setName($name)
    {
        $this->container['name'] = $name;

        return $this;
    }

    /**
     * Gets label
     *
     * @return string
     */
    public function getLabel()
    {
        return $this->container['label'];
    }

    /**
     * Sets label
     *
     * @param string $label a label, that could be used either as a label or as a placeholder on fields supporting it
     *
     * @return $this
     */
    public function setLabel($label)
    {
        $this->container['label'] = $label;

        return $this;
    }

    /**
     * Gets help_text
     *
     * @return string
     */
    public function getHelpText()
    {
        return $this->container['help_text'];
    }

    /**
     * Sets help_text
     *
     * @param string $help_text the text that could be displayed under the field
     *
     * @return $this
     */
    public function setHelpText($help_text)
    {
        $this->container['help_text'] = $help_text;

        return $this;
    }

    /**
     * Gets type
     *
     * @return string
     */
    public function getType()
    {
        return $this->container['type'];
    }

    /**
     * Sets type
     *
     * @param string $type type
     *
     * @return $this
     */
    public function setType($type)
    {
        $allowedValues = $this->getTypeAllowableValues();
        if (!is_null($type) && !in_array($type, $allowedValues, true)) {
            throw new \InvalidArgumentException(
                sprintf(
                    "Invalid value for 'type', must be one of '%s'",
                    implode("', '", $allowedValues)
                )
            );
        }
        $this->container['type'] = $type;

        return $this;
    }

    /**
     * Gets required
     *
     * @return bool
     */
    public function getRequired()
    {
        return $this->container['required'];
    }

    /**
     * Sets required
     *
     * @param bool $required if true, the user has to provide a reply to be able to send the form
     *
     * @return $this
     */
    public function setRequired($required)
    {
        $this->container['required'] = $required;

        return $this;
    }

    /**
     * Gets ordering
     *
     * @return int
     */
    public function getOrdering()
    {
        return $this->container['ordering'];
    }

    /**
     * Sets ordering
     *
     * @param int $ordering the order of this field in the page (smallest is first, null is always smaller). Fields should already be sorted in API responses.
     *
     * @return $this
     */
    public function setOrdering($ordering)
    {
        $this->container['ordering'] = $ordering;

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


