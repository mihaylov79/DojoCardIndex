variable "subscription_id" {
  type        = string
  description = "Your Subscription ID"
}

variable "tenant_id" {
  type = string
  description = "Your Tenant ID"
}

variable "admin_password" {
  type = string
  description = "DB Admin password"
}

variable "cloudinary_cloud_name" {
  type        = string
  description = "Cloudinary Cloud Name"
  sensitive   = true
}

variable "cloudinary_api_key" {
  type        = string
  description = "Cloudinary API Key"
  sensitive   = true
}

variable "cloudinary_api_secret" {
  type        = string
  description = "Cloudinary API Secret"
  sensitive   = true
}

