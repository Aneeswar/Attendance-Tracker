resource "aws_ecr_repository" "attentrack" {
  name                 = "attentrack"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  force_delete = false # Protection against accidental deletion

  tags = {
    Name        = "attentrack-ecr"
    Environment = var.environment
  }
}

# Add lifecycle policy to keep only last 5 images to save storage costs
resource "aws_ecr_lifecycle_policy" "attentrack_policy" {
  repository = aws_ecr_repository.attentrack.name

  policy = <<EOF
{
    "rules": [
        {
            "rulePriority": 1,
            "description": "Keep last 5 images",
            "selection": {
                "tagStatus": "any",
                "countType": "imageCountMoreThan",
                "countNumber": 5
            },
            "action": {
                "type": "expire"
            }
        }
    ]
}
EOF
}
