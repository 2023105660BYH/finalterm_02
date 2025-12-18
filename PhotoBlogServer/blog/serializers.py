from rest_framework import serializers
from .models import Post

class PostSerializer(serializers.ModelSerializer):
    class Meta:
        model = Post
        fields = [
            'id',
            'title',
            'text',
            'image',
            'created_date',
            'published_date',
        ]
        read_only_fields = [
            'created_date',
            'published_date',
        ]
